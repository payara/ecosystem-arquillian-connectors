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

package fish.payara.arquillian.management;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class InjectionTest {
    public static ExternalServerManagement instance;

    static boolean instanceAvailableBeforeDeployment;
    static boolean instanceAvailableInBeforeClass;
    static boolean instanceAvailableInClassRule;
    static boolean programmaticInstanceAvailableInClassRule;

    @ArquillianResource
    ServerManagement management;

    @Deployment
    public static JavaArchive createDeployment() {
        System.out.println("Class deployment called");
        instanceAvailableBeforeDeployment = instance != null;
        return ShrinkWrap.create(JavaArchive.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @BeforeClass
    public static void beforeClass() {
        System.out.println("beforeClass called");
        instanceAvailableInBeforeClass = instance != null;
    }

    @ClassRule
    public static ExternalResource classRule = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            instanceAvailableInClassRule = instance != null;
            programmaticInstanceAvailableInClassRule = ExternalServerManagement.instance() != null;
        }
    };

    @Test
    @RunAsClient
    public void externalServerManagementIsAvailable() {
        assertTrue("ExternalServerManagement instance should be injected in deployment method", instanceAvailableBeforeDeployment);
        // we happen to run this test in embedded container so we're lucky
        assertTrue("ExternalServerManagement instance should be injected in embedded container's @BeforeClass", instanceAvailableInBeforeClass);
        assertFalse("ExternalServerManagement is not injected in @ClassRule, as there's no arquillian callback for that", instanceAvailableInClassRule);
        assertTrue("ExternalServerManagement instance should be available in @ClassRule via programmatic lookup", programmaticInstanceAvailableInClassRule);
    }

    @Test
    public void serverManagementIsAvailable() {
        assertNotNull("ServerManagement instance should be injected via @AruillianResource", management);
        assertNotNull("ServerManagement instance can be looked up programmatically", ServerManagement.instance());
    }

}
