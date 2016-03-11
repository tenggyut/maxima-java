package com.tsinghuabigdata.edu.symbolcompute.maxima;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tsinghuabigdata.common.logging.LogFactory;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the main entry point into Jacomax.
 * <p/>
 * Create one of these Objects using a {@link MaximaConfiguration} to indicate how to run
 * and interact with Maxima. You can then:
 * <ul>
 * <li>
 * Use {@link #launchInteractiveProcess()} to create a Maxima process that you can use
 * in a simple "interactive" mode by sending a number of calls to Maxima and getting
 * results back.
 * </li>
 * </ul>
 * An instance of this class is thread-safe, provided that the {@link MaximaConfiguration}
 * it was created with is not modified.
 *
 * @author tengyt
 */
public class MaximaProcessLauncher {

    private static final Logger logger = LogFactory.getLogger(MaximaProcessLauncher.class);

    /**
     * Default value for {@link MaximaConfiguration#getDefaultCallTimeout()}
     */
    public static final int DEFAULT_CALL_TIMEOUT = 10;

    /**
     * Underlying {@link MaximaConfiguration} used by this launcher
     */
    private final MaximaConfiguration maximaConfiguration;

    /**
     * Creates a new Maxima process launcher, using the given {@link MaximaConfiguration}
     * to specify how to run and connect to Maxima.
     */
    public MaximaProcessLauncher(final MaximaConfiguration maximaConfiguration) {
        Preconditions.checkNotNull(maximaConfiguration, "MaximaConfiguration");
        this.maximaConfiguration = maximaConfiguration;
    }

    /**
     * Launches a new {@link MaximaInteractiveProcess} that you can send individual calls
     * to.
     */
    public MaximaInteractiveProcess launchInteractiveProcess() throws IOException {
        return launchInteractiveProcess(null);
    }

    /**
     * Launches a new {@link MaximaInteractiveProcess} that you can send individual calls
     * to.
     *
     * @param maximaStderrHandler optional OutputStram that will receive any STDERR output
     *                            from Maxima. This may be null, which will result in this output being discarded.
     *                            The caller is reponsible for closing this stream afterwards.
     */
    public MaximaInteractiveProcess launchInteractiveProcess(OutputStream maximaStderrHandler) throws IOException {
        final MaximaInteractiveProcessImpl process = new MaximaInteractiveProcessImpl(newMaximaProcessController(maximaStderrHandler),
                computeDefaultTimeout(maximaConfiguration.getDefaultCallTimeout(), DEFAULT_CALL_TIMEOUT));
        logger.debug("Maxima interactive process started and ready for communication");
        return process;
    }

    private int computeDefaultTimeout(final int configured, final int defaultValue) {
        if (configured > 0) {
            return configured;
        } else if (configured == 0) {
            return defaultValue;
        } else {
            return 0;
        }
    }

    private MaximaProcessController newMaximaProcessController(OutputStream maximaStderrHandler) throws IOException {
        return new MaximaProcessController(this, launchMaximaProcess(), maximaStderrHandler);
    }

    private Process launchMaximaProcess() throws IOException {
        /* Extract relevant configuration required to get Maxima running */
        final String maximaExecutablePath = maximaConfiguration.getMaximaExecutablePath();
        List<String> maximaCommandArguments = maximaConfiguration.getMaximaCommandArguments();
        Map<String, String> maximaRuntimeEnvironment = maximaConfiguration.getMaximaRuntimeEnvironment();

        /* Build up the resulting command that we will execute */
        final List<String> maximaCommandArray = Lists.newLinkedList();
        final Pattern windowsMagicPattern = Pattern.compile("^(.+?\\\\Maxima-([\\d.]+))\\\\bin\\\\maxima.bat$");
        final Matcher windowsMagicMatcher = windowsMagicPattern.matcher(maximaExecutablePath);
        if (windowsMagicMatcher.matches()) {
            /* (We are actually going to directly call the underlying GCL binary that's bundled with
             * the Windows Maxima EXE, which is a bit of a cheat. The reason we do this is so
             * that the Maxima process can be killed if there's a timeout. Otherwise, we'd just
             * be killing the maxima.bat script, which doesn't actually kill the child process on
             * Windows, leaving an orphaned process causing havoc.
             *
             * If you don't want to use GCL here, you'll need to specify the exact Lisp runtime
             * you want and the appropriate command line arguments and environment variables.
             * This information can be gleaned from the maxima.bat script itself.)
             */
            logger.debug("Replacing configured call to Windows Maxima batch file with call to "
                    + "the underlying GCL binary that comes with vanilla Windows Maxima installs. "
                    + "If you don't want this, please adjust your configuration!");
            final String basePath = windowsMagicMatcher.group(1);
            final String versionString = windowsMagicMatcher.group(2);

            maximaCommandArray.add(basePath + "\\lib\\maxima\\" + versionString + "\\binary-gcl\\maxima.exe");
            maximaCommandArray.add("-eval");
            maximaCommandArray.add("(cl-user::run)");
            maximaCommandArray.add("-f");
            if (maximaCommandArguments.size() > 0) {
                maximaCommandArray.add("--");
            }
            if (maximaRuntimeEnvironment == null || maximaRuntimeEnvironment.size() == 0) {
                /* (This makes sure Maxima can find modules and suchlike) */
                maximaRuntimeEnvironment = Maps.newHashMap();
                maximaRuntimeEnvironment.put("MAXIMA_PREFIX", basePath);
            } else {
                logger.warn("I have replaced the maximaExecutablePath in order to invoke the underlying GCL binary."
                        + " I would normally update your maximaRuntimeEnvironment to set MAXIMA_PREFIX"
                        + " but you have already set this, so I'm going with your decision. You may find you"
                        + " need to add a MAXIMA_PREFIX setting to the environment (if you haven't done so already)"
                        + " so that Maxima can find any modules you want to load");
            }
        } else {
            maximaCommandArray.add(maximaExecutablePath);
        }
        if (maximaCommandArguments != null) {
            for (final String arg : maximaCommandArguments) {
                maximaCommandArray.add(arg);
            }
        }

        logger.debug("Starting Maxima cmdarray {} with environment {}", maximaCommandArray, maximaRuntimeEnvironment);
        ProcessBuilder pb = new ProcessBuilder();
        Map<String, String> env = pb.environment();
        env.putAll(maximaConfiguration.getMaximaRuntimeEnvironment());
        pb.command(maximaCommandArray);
        return pb.start();
    }
}
