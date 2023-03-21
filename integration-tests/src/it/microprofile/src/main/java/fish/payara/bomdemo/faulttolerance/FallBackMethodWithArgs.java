package fish.payara.bomdemo.faulttolerance;

import jakarta.enterprise.context.RequestScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@RequestScoped
public class FallBackMethodWithArgs {

    /**
     * Retry 5 times and then fallback
     *
     * @param name
     *            a string
     * @param type
     *            an Integer
     * @return a dummy number
     */
    @Retry(maxRetries = 4)
    @Fallback(fallbackMethod = "fallbackForServiceB")
    public Integer serviceB(String name, Integer type) {
        return 42;
    }

    /**
     * Fallback method with incompatible signature, only one parameter
     *
     * @param name
     *            a string
     * @return a dummy number
     */
    public Integer fallbackForServiceB(String name) {
        return 42;
    }
}
