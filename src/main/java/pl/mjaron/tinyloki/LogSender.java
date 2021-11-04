package pl.mjaron.tinyloki;

import pl.mjaron.tinyloki.third_party.Base64Coder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Implementation of sending bytes to HTTP server.
 */
public class LogSender {
    final LogSenderSettings settings;
    private ILogMonitor logMonitor = null;
    private final URL url;

    /**
     * Creates and configures a new LogSender object.
     *
     * @param settings Parameters required for sending HTTP requests.
     */
    public LogSender(final LogSenderSettings settings) {
        this.settings = settings;
        try {
            this.url = new URL(settings.getUrl());
        } catch (final MalformedURLException e) {
            throw new RuntimeException("Failed to initialize URL with: [" + settings.getUrl() + "].", e);
        }
    }

    /**
     * Getter of {@link LogSenderSettings}.
     *
     * @return {@link LogSenderSettings} used by this log sender.
     */
    public LogSenderSettings getSettings() {
        return settings;
    }

    /**
     * Getter of {@link ILogMonitor}.
     *
     * @return {@link ILogMonitor} used by this log sender.
     */
    @SuppressWarnings("unused")
    public ILogMonitor getLogMonitor() {
        return logMonitor;
    }

    /**
     * Setter of {@link ILogMonitor}.
     * {@link ILogMonitor} object must be set (and not a null) before sending any data with {@link #send(byte[])}.
     *
     * @param logMonitor {@link ILogMonitor} reference.
     */
    public void setLogMonitor(ILogMonitor logMonitor) {
        this.logMonitor = logMonitor;
    }

    /**
     * Creates connection and sends given data by HTTP request.
     * Calls several {@link ILogMonitor} methods pointing what's the request data and HTTP response result.
     *
     * @param message Data to send in HTTP request content.
     */
    void send(final byte[] message) {
        logMonitor.send(message);
        HttpURLConnection connection;
        OutputStream outputStream = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(settings.getConnectTimeout());
            connection.setRequestMethod("POST");
            connection.setRequestProperty("connection", "close");
            connection.setRequestProperty("Content-Type", settings.getContentType());
            connection.setRequestProperty("Content-Length", Integer.toString(message.length));

            if (settings.getUser() != null && settings.getPassword() != null) {
                final String authHeaderContentString = settings.getUser() + ":" + settings.getPassword();
                final String authHeaderEncoded = Base64Coder.encodeString(authHeaderContentString);
                connection.setRequestProperty("Authorization", "Basic " + authHeaderEncoded);
            }

            connection.setAllowUserInteraction(false);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            outputStream = connection.getOutputStream();
            outputStream.write(message);
            outputStream.close();
            outputStream = null;
            final int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                final String responseMessage = connection.getResponseMessage();
                logMonitor.sendErr(responseCode, responseMessage);
            } else {
                logMonitor.sendOk(responseCode);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to send logs.", e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logMonitor.onException(e);
                }
            }
        }
    }
}
