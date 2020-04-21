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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;

class DeployerClient {

    private int port;
    private String contextPath;
    private static final String ENDPOINT = "/application/";

    DeployerClient(int port, String contextPath) {
        this.port = port;
        this.contextPath = contextPath.replaceFirst("/$", "");
    }

    Map<String, String> deploy(String name, InputStream archive) throws IOException {
        URL appUrl = applicationUrl(name);
        HttpURLConnection httpConnection = (HttpURLConnection) appUrl.openConnection();
        try {
            httpConnection.setRequestMethod("PUT");
            httpConnection.addRequestProperty("Content-Type", "application/octet-stream");
            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);
            copy(archive, httpConnection.getOutputStream());
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == 200) {
                return parseOutput(httpConnection.getInputStream());
            } else {
                JsonObject json = Json.createReader(httpConnection.getErrorStream()).readObject();
                String message = json.getString("message", null);
                if (message != null) {
                    if (message.startsWith("CDI definition failure")) {
                        throw new DefinitionException(composeMessageForException(responseCode, message));
                    } else if (message.startsWith("CDI deployment failure") || message.contains("org.jboss.weld.exceptions.DeploymentException")) {
                        throw new DeploymentException(composeMessageForException(responseCode, message));
                    } else {
                        throw new IllegalArgumentException(composeMessageForException(responseCode, message));
                    }
                }
                throw new IllegalArgumentException("Deployment failed. " + this + " returned " + responseCode);
            }
        } finally {
            httpConnection.disconnect();
        }

    }

    private String composeMessageForException(int responseCode, String message) {
        return "Deployment failed. " + this + " returned " + responseCode + " with message: " + message;
    }

    private Map<String, String> parseOutput(InputStream inputStream) {
        JsonObject json = Json.createReader(inputStream).readObject();
        String moduleName = json.getString("name");
        JsonObject moduleDescriptor = json.getJsonObject(moduleName);
        JsonObject mappings = moduleDescriptor.getJsonObject("servletMappings");
        Map<String, String> parsedMappings = new HashMap<>();
        mappings.forEach((path, servlet) -> parsedMappings.put(path, ((JsonString) servlet).getString()));
        return parsedMappings;
    }

    private void copy(InputStream archive, OutputStream sink) throws IOException {
        // not worth pulling another dependency for this
        byte[] buf = new byte[1024];
        int n;
        while ((n = archive.read(buf)) > 0) {
            sink.write(buf, 0, n);
        }
    }

    private URL applicationUrl(String name) {
        try {
            return new URL("http", "localhost", port, contextPath + ENDPOINT + name);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void undeploy(String name) throws IOException {
        URL appUrl = applicationUrl(name);
        HttpURLConnection httpConnection = (HttpURLConnection) appUrl.openConnection();
        try {
            httpConnection.setRequestMethod("DELETE");
            httpConnection.getResponseCode();
        } finally {
            httpConnection.disconnect();
        }
    }

    public String toString() {
        return "Payara Micro Deployer at http://localhost:" + port + contextPath;
    }

}
