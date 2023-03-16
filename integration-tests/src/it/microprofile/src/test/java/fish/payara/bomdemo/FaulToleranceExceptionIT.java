package fish.payara.bomdemo;


import fish.payara.bomdemo.config.EmptyValuesBean;
import fish.payara.bomdemo.faulttolerance.FallBackMethodWithArgs;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.logging.Level;
import java.util.logging.Logger;

@RunWith(Arquillian.class)
public class FaulToleranceExceptionIT {

    private static final Logger log = Logger.getLogger(FaulToleranceExceptionIT.class.getName());

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = "ftDeploymentExceptionTest", managed = false)
    public static WebArchive createFtDeployment() {
        log.log(Level.INFO, "createFtDeployment");
        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "ftInvalid.jar")
            .addClasses(FaulToleranceExceptionIT.class, FallBackMethodWithArgs.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        return ShrinkWrap
            .create(WebArchive.class, "ftInvalidFallbackMethodWithArgs.war")
            .addAsLibrary(testJar)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testFaultToleranceDeploymentException() {
        log.log(Level.INFO, "testFaultToleranceDeploymentException");
        try {
            deployer.deploy("ftDeploymentExceptionTest");
        } catch (FaultToleranceDefinitionException e) {
            Assert.assertTrue(e instanceof FaultToleranceDefinitionException);
        } catch (DefinitionException e) {
            Assert.assertTrue(e instanceof DefinitionException);
        } catch (Exception e) {
            Assert.fail("it throws invalid exception class, " +
                "Expecting either " +
                "org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException" +
                "or " +
                "jakarta.enterprise.inject.spi.DefinitionException. " +
                "FOUND: " + e.getClass());
        }
    }
}
