import ie.ucd.dempsey.mecframework.service.JarController;
import ie.ucd.dempsey.mecframework.service.ServiceController;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A test driver for class {@link ie.ucd.dempsey.mecframework.service.JarController}.
 * <p>
 * This should be ignored by JUnit during the `mvn test` phase.
 */
public class JarControllerTestDriver {
    private static Path jarPath =
            Paths.get("C:\\Users\\demps\\IntelliJProjects\\fyp\\streaming-sample\\streaming-sample-1.jar");

    public static void main(String[] args) {
        ServiceController controller = new JarController(jarPath);
        assert controller.serviceExists();
        assert "streaming-sample-1.jar".equals(controller.name());

        assert !controller.isServiceRunning();

        controller.startService();
        goToSleep();

        assert controller.isServiceRunning();
        controller.stopService();
        goToSleep();

        assert !controller.isServiceRunning();
        controller.shutdown();
    }

    private static void goToSleep() {
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
            ie.printStackTrace();
        }
    }
}
