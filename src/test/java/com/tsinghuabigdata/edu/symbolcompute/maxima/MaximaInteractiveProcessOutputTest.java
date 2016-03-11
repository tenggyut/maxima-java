/* Copyright (c) 2010 - 2012, The University of Edinburgh.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tsinghuabigdata.edu.symbolcompute.maxima;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Additional tests for {@link MaximaInteractiveProcess} that checks the resulting
 * outputs.
 *
 * @author tengyt
 */
public class MaximaInteractiveProcessOutputTest extends MaximaProcessLauncherTest {

    protected MaximaInteractiveProcess maximaInteractiveProcess;

    @Before
    public void setup() throws IOException {
        MaximaProcessLauncher launcher = new MaximaProcessLauncher(MaximaConfiguration.defaultConfig());
        maximaInteractiveProcess = launcher.launchInteractiveProcess();
    }

    @After
    public void cleanup() {
        /* Kill process so we have a clean slate each time */
        if (maximaInteractiveProcess != null) {
            maximaInteractiveProcess.terminate();
            maximaInteractiveProcess = null;
        }
    }

    protected void doSingleOutputCall(final String maximaExpression, final String expectedResult) throws Exception {
        final String result = maximaInteractiveProcess.executeCall(maximaExpression + ";");
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testMultipleCalls() throws Exception {
        for (int i = 0; i < 1000; i++) {
            doSingleOutputCall(String.valueOf(i), String.valueOf(i));
        }
    }

    @Test
    public void testSimpleSingleLine() throws Exception {
        doSingleOutputCall("1", "1");
    }

    @Test
    public void testLessSimpleSingleLine() throws Exception {
        doSingleOutputCall("simp:false$ string(1+x)", "1+x");
    }

    @Test
    public void testSplitSingleLine() throws Exception {
        /* (Maxima splits the raw output, which gets rejoined by our code) */
        doSingleOutputCall("60!", "8320987112741390144276341183223364380754172606361245952449277696409600000000000000");
    }
}
