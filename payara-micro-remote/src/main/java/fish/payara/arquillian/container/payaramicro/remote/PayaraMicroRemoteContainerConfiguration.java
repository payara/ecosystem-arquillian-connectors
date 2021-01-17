/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */
package fish.payara.arquillian.container.payaramicro.remote;

import fish.payara.arquillian.container.payara.CommonPayaraConfiguration;
import org.jboss.arquillian.container.spi.ConfigurationException;


public class PayaraMicroRemoteContainerConfiguration extends CommonPayaraConfiguration {

    private int httpPort = Integer.parseInt(getConfigurableVariable("payara.httpPort", "PAYARA_PORT", "8080"));
    private String deployerContextPath = getConfigurableVariable("payara.deployerPath", "PAYARA_DEPLOYER_PATH", "/payara-micro-deployer");

    /**
     * Get HTTP port of running Payara Micro instance with payara-micro-deployer deployed on it.
     * @return
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * Set HTTP port of running Payara Micro instance with payara-micro-deployer deployed on it.
     * @param httpPort
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * Get path payara-micro-deployer resides at.
     * @return
     */
    public String getDeployerContextPath() {
        return deployerContextPath;
    }

    /**
     * Set context path of payara-micro-deployer. Default value is {@code /micro-deployer}.
     * @param deployerContextPath
     */
    public void setDeployerContextPath(String deployerContextPath) {
        this.deployerContextPath = deployerContextPath;
    }

    @Override
    public void validate() throws ConfigurationException {
        if (httpPort <= 0 || httpPort >= 1 << 16) {
            throw new ConfigurationException("HTTP port is not set");
        }
        if (deployerContextPath == null || deployerContextPath.isEmpty() || !deployerContextPath.startsWith("/")) {
            throw new ConfigurationException("Deployer context path must be specified and start with a slash");
        }
    }

    private static String getConfigurableVariable(String systemPropertyName, String environmentVariableName, String defaultValue) {
        String systemProperty = System.getProperty(systemPropertyName);
        String environmentProperty = System.getenv(environmentVariableName);

        if (systemProperty == null || systemProperty.isEmpty()) {
            if (environmentProperty == null || environmentProperty.isEmpty()) {
                if (defaultValue == null || defaultValue.isEmpty()) {
                    return null;
                }
                return defaultValue;
            }
            return environmentProperty;
        }
        return systemProperty;
    }
}
