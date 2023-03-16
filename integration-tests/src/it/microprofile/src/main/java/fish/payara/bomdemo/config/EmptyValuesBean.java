package fish.payara.bomdemo.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EmptyValuesBean {
    @Inject
    @ConfigProperty(name = "my.unset.property", defaultValue = "")
    private String stringValue;

    public String getStringValue() {
        return stringValue;
    }
}
