/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.arquillian.microdeployer;

import fish.payara.micro.PayaraMicroRuntime;
import fish.payara.micro.data.ApplicationDescriptor;
import fish.payara.micro.data.InstanceDescriptor;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.*;

@RequestScoped
@Path("/application")
public class Deployer {

    private static final Logger LOGGER = Logger.getLogger(Deployer.class.getName());

    @Inject
    PayaraMicroRuntime runtime;

    String errorMessage = null;

    @Path("/{name}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @PUT
    public JsonObject deploy(@PathParam("name") String name, InputStream archive) throws Throwable {
        LOGGER.log(Level.INFO, "Starting deployment of {0}", name);

        errorMessage = null;
        CaptureExceptionHandler handler = new CaptureExceptionHandler();

        Logger serverLogger = LogManager.getLogManager().getLogger("javax.enterprise.system.core");
        handler.addToLogger(serverLogger);

        boolean deployOk = false;
        try {
            deployOk = runtime.deploy(name, archive);
        } finally {
            handler.removeFromLogger(serverLogger);
            errorMessage = handler.deploymentExceptionMessage;
        }

        if (!deployOk) {
            throw badRequest();
        }

        JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
        InstanceDescriptor instance = runtime.getLocalDescriptor();

        ApplicationDescriptor descriptor = instance.getDeployedApplications().stream().filter(a -> a.getName().equals(name))
                .findFirst().orElseThrow(this::badRequest);

        resultBuilder.add("name", name)
                .add("httpPorts", Json.createArrayBuilder(instance.getHttpPorts()));

        descriptor.getModuleDescriptors().forEach(moduleDescriptor -> {
            JsonObjectBuilder moduleBuilder = Json.createObjectBuilder();
            moduleBuilder.add("type", moduleDescriptor.getType())
                    // createObjectBuilder should apparently accept Map<String,?> rather than Map<String,Object>.
                    // https://github.com/eclipse-ee4j/jsonp/issues/168
                .add("servletMappings", Json.createObjectBuilder((Map)moduleDescriptor.getServletMappings()));
            resultBuilder.add(moduleDescriptor.getName(), moduleBuilder.build());
        });
        return resultBuilder.build();
    }

    private ClientErrorException badRequest() {
        if (errorMessage != null) {
            return badRequest(errorMessage);
        } else {
            return badRequest("Application failed to deploy. Check logs");
        }
    }

    private ClientErrorException badRequest(String message) {
        JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
        if (message != null) {
            resultBuilder.add("message", message);
        }
        return new ClientErrorException(Response.status(Response.Status.BAD_REQUEST)
                .entity(resultBuilder.build())
                .build());
    }

    @Path("/{name}")
    @DELETE
    public void undeploy(@PathParam("name") String name) {
        LOGGER.log(Level.INFO, "Starting undeployment of {0}", name);
        runtime.undeploy(name);
    }

    private static class CaptureExceptionHandler extends Handler {

        String deploymentExceptionMessage;

        @Override
        public void publish(LogRecord record) {
            if (record.getThrown() != null
                    && record.getThrown().getClass().getSimpleName().equals("DeploymentException")) {
                deploymentExceptionMessage = record.getThrown().getMessage();
                LOGGER.log(Level.FINEST, "DeploymentException detected, message: {0}", deploymentExceptionMessage);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        public void addToLogger(Logger serverLogger) throws SecurityException {
            if (serverLogger != null) {
                LOGGER.fine("Found server logger, adding handler");
                serverLogger.addHandler(this);
            } else {
                LOGGER.warning("Didn't find server logger, won't be able to detect information about deployment errors");
            }
        }

        public void removeFromLogger(Logger serverLogger) throws SecurityException {
            if (serverLogger != null) {
                LOGGER.fine("Removing handler from server logger");
                serverLogger.removeHandler(this);
                LOGGER.log(Level.FINER, "Error message: {0}", deploymentExceptionMessage);
            }
        }

    }

}
