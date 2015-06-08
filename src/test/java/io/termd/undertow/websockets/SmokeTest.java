package io.termd.undertow.websockets;

import io.termd.core.util.ObjectWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class SmokeTest {

    private static final Logger log = LoggerFactory.getLogger(SmokeTest.class);

    private static final String HOST = "localhost";
    private static final String WEB_SOCKET_PATH = "/term";
    private static final int PORT = 8080;

    @BeforeClass
    public static void setUP() throws Exception {
        TermdServer.startServer(HOST, PORT);
    }

    @AfterClass
    public static void tearDown() {
        TermdServer.stopServer();
    }

    @Test
    public void serverShouldBeUpAndRunning() throws Exception {
        String content = readUrl(HOST, PORT, "/");
        Assert.assertTrue("Cannot read response from serverThread.", content.length() > 0);
    }

    @Test
    public void clientShouldBeAbleToOpenWebSocketConnection() throws Exception {
        String webSocketUrl = "http://" + HOST + ":" + PORT + WEB_SOCKET_PATH;

        ObjectWrapper<Boolean> pwdExecuted = new ObjectWrapper<>(false);
        ObjectWrapper<List<String>> remoteResponseWrapper = new ObjectWrapper<>(new ArrayList<>());

        Client client = setUpClient();
        Consumer<byte[]> responseConsumer = (bytes) -> {
            String response = new String(bytes);
            if ("% ".equals(response)) {
                if (!pwdExecuted.get()) {
                    pwdExecuted.set(true);
                    executeRemoteCommand(client, "java -help");
                }
            } else {
                remoteResponseWrapper.get().add(response);
            }
        };
        client.onBinaryMessage(responseConsumer);

        client.onClose(closeReason -> {
        });

        try {
            client.connect(webSocketUrl);
        } catch (Exception e) {
            throw new AssertionError("Failed to connect to remote client.", e);
        }

        assertThatResultWasReceived(remoteResponseWrapper, 5, ChronoUnit.SECONDS);

        assertThatCommandCompletedSuccessfully(5, ChronoUnit.SECONDS);

        client.close();
    }

    private void assertThatResultWasReceived(ObjectWrapper<List<String>> remoteResponseWrapper, long timeout, TemporalUnit timeUnit) throws InterruptedException {
        List<String> strings = remoteResponseWrapper.get();

        boolean responseContainsExpectedString = false;
        LocalDateTime stared = LocalDateTime.now();
        while (true) {
            List<String> stringsCopy = new ArrayList<>(strings);
            String remoteResponses = stringsCopy.stream().collect(Collectors.joining());

            if (stared.plus(timeout, timeUnit).isBefore(LocalDateTime.now())) {
                log.info("Remote responses: {}", remoteResponses);
                throw new AssertionError("Did not received response in " + timeout + " " + timeUnit);
            }

            if (remoteResponses.contains("-classpath")) {
                responseContainsExpectedString = true;
                log.info("Remote responses: {}", remoteResponses);
                break;
            } else {
                Thread.sleep(200);
            }
        }
        Assert.assertTrue("Response should contain current working dir.", responseContainsExpectedString);
    }

    private void assertThatCommandCompletedSuccessfully(long timeout, TemporalUnit timeUnit) throws InterruptedException {
//
//        boolean responseContainsExpectedString = false;
//        LocalDateTime stared = LocalDateTime.now();
//        while (true) {
//            List<String> stringsCopy = new ArrayList<>(strings);
//            String remoteResponses = stringsCopy.stream().collect(Collectors.joining());
//
//            if (stared.plus(timeout, timeUnit).isBefore(LocalDateTime.now())) {
//                log.info("Remote responses: {}", remoteResponses);
//                throw new AssertionError("Did not received response in " + timeout + " " + timeUnit);
//            }
//
//            if (remoteResponses.contains("-classpath")) {
//                responseContainsExpectedString = true;
//                log.info("Remote responses: {}", remoteResponses);
//                break;
//            } else {
//                Thread.sleep(200);
//            }
//        }
//        Assert.assertTrue("Response should contain current working dir.", responseContainsExpectedString);
    }

    private void executeRemoteCommand(Client client, String command) {
        log.info("Executing remote command ...");
        RemoteEndpoint.Basic remoteEndpoint = client.getRemoteEndpoint();
        String data = "{\"action\":\"read\",\"data\":\"" + command + "\\r\\n\"}";
        try {
            remoteEndpoint.sendBinary(ByteBuffer.wrap(data.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Client setUpClient() {
        Client client = new Client();

        Consumer<Session> onOpen = (session) -> {
            log.info("Client connection opened.");
        };

        Consumer<CloseReason> onClose = (closeReason) -> {
            log.info("Client connection closed. " + closeReason);
        };

        client.onOpen(onOpen);
        client.onClose(onClose);

        return client;
    }

    private String readUrl(String host, int port, String path) throws IOException {
        URL url = new URL("http://" + host + ":" + port + path);
        URLConnection connection = url.openConnection();
        connection.connect();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String inputLine;
        StringBuilder stringBuilder = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null) {
            stringBuilder.append(inputLine);
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }

}
