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

import com.google.common.base.Stopwatch;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Base class for integration tests of the {@link MaximaProcessLauncher} class.
 *
 * @author tengyt
 */
public class MaximaProcessLauncherTest {

    @Test
    public void testCallingProcess() throws IOException {
        final MaximaConfiguration configuration = MaximaConfiguration.defaultConfig();
        final MaximaProcessLauncher launcher = new MaximaProcessLauncher(configuration);
        final MaximaInteractiveProcess process = launcher.launchInteractiveProcess();
        process.executeCallDiscardOutput("display2d:false;linel:1024;", 1);
        Stopwatch timer = Stopwatch.createStarted();
        for (int i = 0; i < 1; i++) {


            final String result = process.executeCall("solve(x^2+2*x+1=0);", 5);
            final String result2 = process.executeCall("expand((y-1)^2);", 5);

            System.out.println("Result is: " + result);
            System.out.println("Result is: " + result2);

            process.softReset();
        }
        System.out.println("cost : " + timer.elapsed(TimeUnit.SECONDS));
    }
}
