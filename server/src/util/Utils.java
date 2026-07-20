// OBITO
package util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Utils {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public static void setTimeout(Runnable runnable, int delay) {
        scheduler.schedule(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                System.err.println(e);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
}
