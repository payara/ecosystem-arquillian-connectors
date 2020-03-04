/*
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 */
package fish.payara.arquillian.container.payara.process;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CloseableProcess extends Process implements AutoCloseable {

    private final Process wrapped;

    public CloseableProcess(Process process) {
        wrapped = process;
    }

    public Process getWrapped() {
        return wrapped;
    }

    @Override
    public InputStream getInputStream() {
        return getWrapped().getInputStream();
    }

    @Override
    public OutputStream getOutputStream() {
        return getWrapped().getOutputStream();
    }

    @Override
    public InputStream getErrorStream() {
        return getWrapped().getErrorStream();
    }

    public boolean isAlive() {
        try {
            getWrapped().exitValue();
            return false;
        } catch(IllegalThreadStateException e) {
            return true;
        }
    }

    @Override
    public int waitFor() throws InterruptedException {
        return getWrapped().waitFor();
    }

    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        
        // while the given time hasn't elapsed, check if the process has died
        // if it has, return true - else return false when time elapsed
        Timer timer = new Timer();
        final float timeToStop = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, unit);
        final AtomicReference<Boolean> threadStopped = new AtomicReference<Boolean>(false);
        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(isAlive()) {
                    threadStopped.set(true);
                    cancel();
                }
                if(System.currentTimeMillis() >= timeToStop) {
                    cancel();
                }
            }
        }, 0, 100);
        
        return threadStopped.get();
    }

    @Override
    public void close() {
        destroy();
    }

    @Override
    public void destroy() {
        getWrapped().destroy();
    }

    public Process destroyForcibly() {
        Process destroyed = getWrapped();
        getWrapped().destroy();
        return destroyed;
    }

    @Override
    public int exitValue() {
        return getWrapped().exitValue();
    }

}
