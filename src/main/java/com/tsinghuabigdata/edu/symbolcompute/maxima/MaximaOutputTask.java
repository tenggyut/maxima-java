package com.tsinghuabigdata.edu.symbolcompute.maxima;

import com.google.common.collect.Lists;
import com.tsinghuabigdata.common.utils.StringCollectionUtils;
import org.apache.logging.log4j.Logger;
import com.tsinghuabigdata.common.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * output read worker
 * Created by tenggyt on 2016/3/11.
 */
public class MaximaOutputTask implements Callable<String> {
    private static final Logger LOG = LogFactory.getLogger(MaximaOutputTask.class);

    private static final int MAX_TRY = 3;

    private int timeout;
    private final InputStream maximaStdout;

    public MaximaOutputTask(InputStream maximaStdout, int timeout) {
        this.timeout = timeout;
        this.maximaStdout = maximaStdout;
    }

    @Override
    public String call() throws IOException {
        return doMaximaReadLoop();
    }

    private String doMaximaReadLoop() throws IOException {
        List<String> line = Lists.newLinkedList();
        synchronized (maximaStdout) {
            InputStreamReader isr = new InputStreamReader(maximaStdout);
            BufferedReader br = new BufferedReader(isr);
            int retryCount = 0;
            while (maximaStdout.available() > 0 || retryCount < MAX_TRY) {
                if (maximaStdout.available() <= 0) {
                    int sleep = retryCount == 0 ? 50 : (timeout * 1000) / MAX_TRY;
                    try {
                        Thread.sleep(sleep);
                        retryCount++;
                    } catch (InterruptedException e) {
                        LOG.error(e);
                    }
                } else {
                    line.add(br.readLine().trim());
                    break;
                }
            }
        }
        return StringCollectionUtils.join(line, System.lineSeparator());
    }
}
