package com.tsinghuabigdata.edu.symbolcompute.maxima;

import com.tsinghuabigdata.common.logging.LogFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.*;

/**
 * Provides basic I/O functionality for a Maxima process.
 * <p/>
 * {@link MaximaInteractiveProcessImpl} use this
 * to do their work.
 *
 * @author tengyt
 */
public class MaximaProcessController {

    static final Logger LOG = LogFactory.getLogger(MaximaProcessController.class);

    /**
     * Time to wait after asking Maxima process to terminate before forcibly killing it.
     * (This is needed if Maxima gets locked in a calculation that is either very
     * complex or will never actually finish.)
     */
    private static final int PROCESS_KILL_TIMEOUT = 1;

    /**
     * {@link MaximaProcessLauncher} owning this
     */
    private final MaximaProcessLauncher launcher;

    /**
     * Helper to manage asynchronous calls to Maxima process thread
     */
    private final ExecutorService executor;

    /**
     * Maxima {@link Process} encapsulated by this
     */
    final Process maximaProcess;

    /**
     * Maxima STDIN handle
     */
    final OutputStream maximaStdin;

    /**
     * Maxima STDOUT handle
     */
    final InputStream maximaStdout;

    /**
     * Maxima STDERR handle
     */
    final InputStream maximaStderr;

    /**
     * Handles Maxima STDERR (may be null)
     */
    final OutputStream maximaStderrHandler;

    /**
     * Flag set when the underlying process has been terminated
     */
    private boolean terminated;

    public MaximaProcessController(final MaximaProcessLauncher launcher, final Process maximaProcess, final OutputStream maximaStderrHandler) {
        this.launcher = launcher;
        this.maximaProcess = maximaProcess;
        this.maximaStderrHandler = maximaStderrHandler;
        this.executor = Executors.newFixedThreadPool(3); /* (stdin, stdout, stderr, shutdown) */
        this.maximaStdout = maximaProcess.getInputStream();
        this.maximaStderr = maximaProcess.getErrorStream();
        this.maximaStdin = maximaProcess.getOutputStream();
        this.terminated = false;
    }

    public boolean isTerminated() {
        return terminated;
    }

    /**
     * Terminates the underlying Maxima process, forcibly if required. No
     * more calls can be made to this process after this point.
     * <p/>
     * Calling this on a process that has already terminated will do nothing.
     *
     * @return underlying exit value from the Maxima process, {@link MaximaInteractiveProcess#PROCESS_ALREADY_TERMINATED}
     * if the process was already terminated, or {@link MaximaInteractiveProcess#PROCESS_FORCIBLY_DESTROYED}
     * if the process had to be forcibly destroyed.
     */
    public int terminate() {
        if (terminated) {
            return MaximaInteractiveProcess.PROCESS_ALREADY_TERMINATED;
        }

        /* Then terminate the Maxima process */
        return terminateMaximaProcess();
    }

    /* (Thread safe) */
    private int terminateMaximaProcess() {
        terminated = true;
        try {
            try {
                /* Ask Maxima to nicely close down by closing its input */
                LOG.debug("Attempting to close Maxima nicely");
                synchronized (maximaStdin) {
                    maximaStdin.close();
                }
                final FutureTask<Integer> shutdownTask = new FutureTask<>(new Callable<Integer>() {
                    public Integer call() throws Exception {
                        return maximaProcess.waitFor();
                    }
                });
                executor.execute(shutdownTask);
                return shutdownTask.get(PROCESS_KILL_TIMEOUT, TimeUnit.SECONDS);
            } catch (final Exception e) {
                LOG.debug("Maxima process did not terminate naturally, so forcibly terminating", e);
                maximaProcess.destroy();
                return MaximaInteractiveProcess.PROCESS_FORCIBLY_DESTROYED;
            }
        } finally {
            executor.shutdown();
            if (maximaStderrHandler != null) {
                try {
                    maximaStderrHandler.close();
                } catch (IOException e) {
                    LOG.error(e);
                }
            }
        }
    }

    public String doMaximaCall(String cmd, int callTimeout) {
        ensureNotTerminated();
        try {
            String raw = "";
            if (callTimeout > 0) {
                /* Wait until timeout */
                LOG.trace("Invoking maxima call using timeout {}s", callTimeout);
                try {
                    doMaximaWriteLoop(cmd);
                } catch (IOException e) {
                    LOG.debug("Timeout was exceeded communicating with Maxima - terminating the process");
                    terminateMaximaProcess();
                }

                Future<String> outputFuture = executor.submit(new MaximaOutputTask(maximaStdout, callTimeout));
                raw = outputFuture.get();
            }
            return raw;
        } catch (final ExecutionException e) {
            terminateMaximaProcess();
        } catch (final InterruptedException e) {
            if (!terminated) {
                LOG.debug("Maxima threads interrupted unexpectedly - terminating the process");
                terminateMaximaProcess();
            }
        }
        return "";
    }

    private void ensureNotTerminated() {
        if (terminated) {
            throw new IllegalStateException();
        }
    }

    private void doMaximaWriteLoop(String callInputStream) throws IOException {
        if (StringUtils.isBlank(callInputStream)) {
            LOG.trace("Maxim STDIN loop exiting immediately as callInputStream is null");
            return;
        }
        synchronized (maximaStdin) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("MAXIMA>>>: {}", callInputStream);
            }
            maximaStdin.write(callInputStream.getBytes());
            maximaStdin.flush();
        }
    }
}