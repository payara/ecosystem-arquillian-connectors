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

package fish.payara.bomdemo;

import fish.payara.bomdemo.config.EmptyValuesBean;
import fish.payara.bomdemo.faulttolerance.FallBackMethodWithArgs;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class WholeAppIT {
    @Deployment
    public static WebArchive deployWholeApp() throws IOException {
        Path packageResult = Files.list(Paths.get("target"))
                .filter(p -> p.getFileName().toString().endsWith(".war"))
                .findAny().orElseThrow(() -> new IllegalStateException("No .war file in target directory. Run 'mvn verify'"));
        return ShrinkWrap.createFromZipFile(WebArchive.class, packageResult.toFile())
            .deleteClass(FallBackMethodWithArgs.class)
            .deleteClass(EmptyValuesBean.class);
    }

    @ArquillianResource
    URI baseUrl;

    @Test
    public void helloControllerGreets() {
        String greeting = ClientBuilder.newClient().target(baseUrl).path("data/hello").request().get(String.class);
        assertEquals("Hello World", greeting);
    }

    @Test
    public void healthCheckUp() {
        WebTarget healthPath = ClientBuilder.newClient().target(baseUrl.resolve("/")).path("health");
        JsonObject health = healthPath
                .request(MediaType.APPLICATION_JSON_TYPE).get(JsonObject.class);
        assertNotNull(health);
        System.out.println(health);
        // response changed with Health 2.2 / 5.194, so let's not go into details of the message
    }

    @Test
    public void metricMeasures() {
        Client client = ClientBuilder.newClient();
        WebTarget metricEndpoint = client.target(baseUrl).path("data/metric");

        long counter = metricEndpoint.path("increment").request().get(Long.class);
        assertTrue("counter should be at least 1, is " + counter, counter > 0);

        WebTarget metricsPath = client.target(baseUrl.resolve("/")).path("metrics");
        JsonObject metrics = metricsPath
                .request(MediaType.APPLICATION_JSON_TYPE).get(JsonObject.class);
        assertNotNull(metrics);
        /*
        {
          "base": {
            // ...
          },
          "application": {
            "fish.payara.bomdemo.metric.MetricController.endpoint_counter;_app=microprofile-test-3.0.alpha6-snapshot": 1,
            "fish.payara.bomdemo.metric.MetricController.timed-request": {
              "elapsedTime;_app=microprofile-test-3.0.alpha6-snapshot": 0,
              "count;_app=microprofile-test-3.0.alpha6-snapshot": 0,
              "meanRate;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "oneMinRate;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "fiveMinRate;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "fifteenMinRate;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "min;_app=microprofile-test-3.0.alpha6-snapshot": 0,
              "max;_app=microprofile-test-3.0.alpha6-snapshot": 0,
              "mean;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "stddev;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "p50;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "p75;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "p95;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "p98;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "p99;_app=microprofile-test-3.0.alpha6-snapshot": 0.0,
              "p999;_app=microprofile-test-3.0.alpha6-snapshot": 0.0
            }
          }
        }
        */
        JsonObject app = metrics.getJsonObject("application");
        String key = app.keySet().stream().filter(s -> s.contains("endpoint_counter")).findFirst().orElseThrow(() -> new AssertionError(
                "endpoint_counter should be present in " + app));
        assertEquals(counter,
                metrics.getJsonObject("application").getJsonNumber(key).longValue());
        assertNotNull(metrics.getJsonObject("application").getJsonObject("fish.payara.bomdemo.metric.MetricController.timed-request"));
    }

    @Test
    public void configConfigures() {
        Client client = ClientBuilder.newClient();
        WebTarget configEndpoint = client.target(baseUrl).path("data/config/");

        String configResult = configEndpoint.path("injected").request().get(String.class);
        assertEquals("Config value as Injected by CDI Injected value", configResult);
    }
}
