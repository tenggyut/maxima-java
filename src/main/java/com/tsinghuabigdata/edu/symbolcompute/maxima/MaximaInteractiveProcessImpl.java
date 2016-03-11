package com.tsinghuabigdata.edu.symbolcompute.maxima;

import com.google.common.base.Preconditions;
import com.tsinghuabigdata.common.logging.LogFactory;
import org.apache.logging.log4j.Logger;

/**
 * This is the internal implementation of {@link MaximaInteractiveProcess}.
 *
 * @author tengyt
 */
public class MaximaInteractiveProcessImpl implements MaximaInteractiveProcess {
    private static final Logger LOG = LogFactory.getLogger(MaximaInteractiveProcessImpl.class);

    private final MaximaProcessController maximaProcessController;
    private int defaultCallTimeout;

    public MaximaInteractiveProcessImpl(final MaximaProcessController maximaProcessController, final int defaultCallTimeout) {
        this.maximaProcessController = maximaProcessController;
        this.defaultCallTimeout = defaultCallTimeout;
    }

    public String executeCall(final String callInput) {
        return executeCall(callInput, defaultCallTimeout);
    }

    public String executeCall(final String callInput, final int callTimeout) {
        LOG.debug("executeCall(input={}, timeout={})", callInput, callTimeout);
        Preconditions.checkNotNull(callInput, "maximaInput");
        ensureNotTerminated();

        String rawOutput = maximaProcessController.doMaximaCall(callInput, callTimeout);

        LOG.debug("{} => {}", callInput, rawOutput);
        return rawOutput;
    }

    public void executeCallDiscardOutput(final String callInput) {
        executeCallDiscardOutput(callInput, defaultCallTimeout);
    }

    public void executeCallDiscardOutput(final String callInput, final int callTimeout) {
        LOG.debug("executeCallDiscardOutput(input={}, timeout={})", callInput, callTimeout);
        Preconditions.checkNotNull(callInput, "maximaInput");
        ensureNotTerminated();

        maximaProcessController.doMaximaCall(callInput, callTimeout);
    }

    public void softReset() {
        executeCallDiscardOutput("[kill(all),reset()]$");
    }

    public int terminate() {
        return maximaProcessController.terminate();
    }

    public boolean isTerminated() {
        return maximaProcessController.isTerminated();
    }

    private void ensureNotTerminated() {
        if (isTerminated()) {
            throw new IllegalStateException();
        }
    }
}
