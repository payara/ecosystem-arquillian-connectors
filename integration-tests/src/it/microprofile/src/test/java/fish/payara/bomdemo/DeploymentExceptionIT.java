package fish.payara.bomdemo;

import fish.payara.bomdemo.config.EmptyValuesBean;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.DefinitionException;
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
public class DeploymentExceptionIT {

    private static final Logger log = Logger.getLogger(DeploymentExceptionIT.class.getName());

    private static final String PROP_FILE_EMPTY_PROPERTY = "my.empty.property.in.config.file";

    public static final StringAsset EMPTY_STRING_ASSET = new StringAsset(PROP_FILE_EMPTY_PROPERTY + "=");

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = "mpConfigDeploymentExceptionTest", managed = false)
    public static Archive createMpConfigDeployment() {
        log.log(Level.INFO, "createMpConfigDeployment");
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "emptyValues.jar")
            .addClasses(FaulToleranceExceptionIT.class, EmptyValuesBean.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsManifestResource(EMPTY_STRING_ASSET, "microprofile-config.properties");

        return ShrinkWrap.create(WebArchive.class)
            .addAsLibrary(jar)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testMpConfigDeploymentException() {
        log.log(Level.INFO, "testMpConfigDeploymentException");
        try {
            deployer.deploy("mpConfigDeploymentExceptionTest");
        } catch (DeploymentException e) {
            Assert.assertTrue(e instanceof DeploymentException);
        } catch (DefinitionException e) {
            Assert.assertTrue(e instanceof DefinitionException);
        } catch (Exception e) {
            Assert.fail("it throws invalid exception class, " +
                "Expecting either " +
                "jakarta.enterprise.inject.spi.DeploymentException. " +
                "or " +
                "jakarta.enterprise.inject.spi.DefinitionException. " +
                "FOUND: " + e.getClass());
        }
    }
}