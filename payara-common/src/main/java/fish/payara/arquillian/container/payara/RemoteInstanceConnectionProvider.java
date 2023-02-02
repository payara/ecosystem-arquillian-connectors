package fish.payara.arquillian.container.payara;

import java.util.Optional;

/**
 * This interface abstracts getting details for a remote Payara instance configuration,
 * which might override information retrieved from the instance via the admin API.
 */
public interface RemoteInstanceConnectionProvider {
    
    Optional<String> getHttpHost();
    Optional<Integer> getHttpPort();
    
}
