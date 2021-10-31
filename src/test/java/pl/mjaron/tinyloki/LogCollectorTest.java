package pl.mjaron.tinyloki;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

class LogCollectorTest {

    @Test
    void basicJson() {
        JsonLogCollector collector = new JsonLogCollector();
        Map<String, String> labels = new TreeMap<>();
        labels.put("level", "INFO");
        labels.put("host", "ZEUS");
        ILogStream stream = collector.createStream(labels);
        stream.log(System.currentTimeMillis(), "Hello world.");
        final String collected = collector.collectAsString();
        System.out.println("Collected:\n" + collected);
        LogSender logSender = new LogSender(LogSenderSettings.create()
                .setUrl("http://localhost/loki/api/v1/push")
                .setUser("user")
                .setPassword("pass")
                .setContentType(collector.contentType()));
        logSender.send(collected.getBytes(StandardCharsets.UTF_8));
        System.out.println("All done.");
    }

    @Test
    void logController() {
        LogController logController = new LogController(new JsonLogCollector(),
                LogSenderSettings.create()
                        .setUrl("http://localhost/loki/api/v1/push")
                        .setUser("user")
                        .setPassword("pass")).start();
        Map<String, String> labels = new TreeMap<>();
        labels.put("level", "INFO");
        labels.put("host", "ZEUS");
        ILogStream stream = logController.createStream(labels);
        stream.log(System.currentTimeMillis(), "Hello world.");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logController.softStop().waitForStop();
    }
}