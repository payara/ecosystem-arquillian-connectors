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

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import java.io.IOException;
import java.util.Map;

public class PayaraMicroRemoteDeployableContainer implements DeployableContainer<PayaraMicroRemoteContainerConfiguration> {
    private PayaraMicroRemoteContainerConfiguration configuration;
    private DeployerClient deployer;

    @Override
    public Class<PayaraMicroRemoteContainerConfiguration> getConfigurationClass() {
        return PayaraMicroRemoteContainerConfiguration.class;
    }

    @Override
    public void setup(PayaraMicroRemoteContainerConfiguration configuration) {
        this.configuration = configuration;
        this.deployer = new DeployerClient(configuration.getHttpPort(), configuration.getDeployerContextPath());
    }

    @Override
    public void start() throws LifecycleException {
        // we don't manage the server
    }

    @Override
    public void stop() throws LifecycleException {
        // we don't manage the server
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 4.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        try {
            String baseName = baseNameOf(archive);
            Map<String, String> mappings = deployer.deploy(baseName, archive.as(ZipExporter.class).exportAsInputStream());
            ProtocolMetaData metadata = new ProtocolMetaData();
            HTTPContext httpContext = new HTTPContext(baseName, "localhost", configuration.getHttpPort());
            mappings.forEach((path, servlet) -> {
                httpContext.add(new Servlet(servlet, baseName));
            });
            metadata.addContext(httpContext);
            return metadata;
        } catch (IOException e) {
            throw new DeploymentException("Failed to deploy archive to "+deployer, e);
        }
    }

    private static String baseNameOf(Archive<?> archive) {
        return archive.getName().replaceFirst("\\.\\w+$","");
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        try {
            deployer.undeploy(baseNameOf(archive));
        } catch (IOException e) {
            throw new DeploymentException("Failed to undeploy archive from "+deployer, e);
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }


}
