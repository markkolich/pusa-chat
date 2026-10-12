/*
 * Copyright (c) 2026 Mark S. Kolich
 * https://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package pusachat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.ConfigFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Boots the whole stack exactly as {@link Application} does, on an ephemeral port, and drives
 * the chat API the way the room page's JavaScript does.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // One server for all tests in the class.
public final class PusaChatEndToEndTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /**
     * The timestamp shapes the front-end's date parser (jquery.localtime) accepts: with
     * milliseconds, or without when they're exactly zero.
     */
    private static final Pattern TIMESTAMP = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z");

    private static final long EVENT_WAIT_MS = 15_000L;

    private Server server_;

    private String baseUri_;

    @BeforeAll
    public void startServer() throws Exception {
        // Curacao loads its config (and ours) through ConfigFactory.load() at context startup.
        System.setProperty("config.resource", "pusachat-test.conf");
        ConfigFactory.invalidateCaches();

        server_ = Application.buildServer("127.0.0.1", 0);
        server_.start();

        final int port = ((ServerConnector) server_.getConnectors()[0]).getLocalPort();
        baseUri_ = "http://127.0.0.1:" + port + "/pusa-chat";
    }

    @AfterAll
    public void stopServer() throws Exception {
        if (server_ != null) {
            server_.stop();
        }
        System.clearProperty("config.resource");
        ConfigFactory.invalidateCaches();
    }

    // Pages and static assets

    @Test
    public void homepageRendersWithEscapedRoomKey() throws Exception {
        final HttpResponse<String> response = get("/?key=" + URLEncoder.encode("<b>x</b>", StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
        assertTrue(response.body().contains("data-context-path=\"/pusa-chat\""), response.body());
        assertTrue(response.body().contains("value=\"&lt;b&gt;x&lt;/b&gt;\""), response.body());
        assertTrue(response.body().contains("/pusa-chat/static/release/app.min.css?v="), response.body());
        assertTrue(response.body().contains("/pusa-chat/static/release/app.min.js?v="), response.body());
    }

    @Test
    public void staticAssetsAreServed() throws Exception {
        final List<String> paths = List.of("/static/release/app.min.js", "/static/release/app.min.css",
                "/static/img/delete.gif", "/favicon.ico", "/robots.txt");
        for (final String path : paths) {
            assertEquals(200, get(path).statusCode(), path);
        }
        // The gulp build concatenates every script into one minified file, banner first.
        final String script = get("/static/release/app.min.js").body();
        assertTrue(script.startsWith("/*\n * (c) Copyright"), script.substring(0, 100));
        assertTrue(script.contains("jQuery v1.7.1"), "Missing jQuery");
        assertTrue(script.contains("chat/event/"), "Missing the chat app");
    }

    @Test
    public void unknownPathsAreNotFound() throws Exception {
        final HttpResponse<String> page = get("/nope");
        assertEquals(404, page.statusCode());
        assertTrue(page.body().contains("404 Not Found"), page.body());

        assertEquals(404, get("/api/nope.json").statusCode());
    }

    @Test
    public void roomPageRendersForRoomToken() throws Exception {
        final JsonNode room = createRoom("Page-Room");

        final HttpResponse<String> page = get("/api/room/" + room.get("token").asText());

        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("<title>page-room</title>"), page.body());
        assertTrue(page.body().contains("id=\"" + room.get("token").asText() + "\""), page.body());
    }

    @Test
    public void roomPageWithForgedTokenIsBadRequest() throws Exception {
        final String token = createRoom("forged-room").get("token").asText();
        final String forged = token.substring(0, token.length() - 1) + (token.endsWith("0") ? "1" : "0");

        final HttpResponse<String> page = get("/api/room/" + forged);

        assertEquals(400, page.statusCode());
        assertTrue(page.body().contains("400 Bad Request"), page.body());
    }

    // Rooms

    @Test
    public void createJoinAndRandomRooms() throws Exception {
        final JsonNode created = createRoom("My-Room");
        assertEquals("my-room", created.get("name").asText());
        assertTrue(TIMESTAMP.matcher(created.get("createdOn").asText()).matches(), created.toString());

        // Creating it again, or joining it, returns the same room.
        assertEquals(created.get("token"), createRoom("my-room").get("token"));
        final HttpResponse<String> joined = post("/api/room/join.json", Map.of("name", "MY-ROOM"));
        assertEquals(200, joined.statusCode());
        assertEquals(created.get("token"), MAPPER.readTree(joined.body()).get("token"));

        final HttpResponse<String> random = get("/api/room/random.json");
        assertEquals(200, random.statusCode());
        assertTrue(MAPPER.readTree(random.body()).get("name").asText().matches("[a-z0-9]{10}"), random.body());
    }

    @Test
    public void joiningMissingRoomIsNotFound() throws Exception {
        final HttpResponse<String> response = post("/api/room/join.json", Map.of("name", "no-such-room"));

        assertEquals(404, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertTrue(MAPPER.readTree(response.body()).has("message"), response.body());
    }

    @Test
    public void invalidOrMissingRoomNamesAreBadRequests() throws Exception {
        assertEquals(400, post("/api/room/create.json", Map.of("name", "spaces are bad")).statusCode());
        assertEquals(400, post("/api/room/create.json", Map.of("name", "x".repeat(37))).statusCode());
        assertEquals(400, post("/api/room/create.json", Map.of()).statusCode());
    }

    // Chat

    @Test
    public void clientsChatThroughTheRoom() throws Exception {
        final String roomToken = createRoom("chat-room").get("token").asText();

        final JsonNode alice = register(roomToken);
        assertTrue(TIMESTAMP.matcher(alice.get("lastAccessed").asText()).matches(), alice.toString());
        final String aliceToken = alice.get("token").asText();
        final JsonNode bob = register(roomToken);
        final String bobToken = bob.get("token").asText();
        final String bobId = bob.get("clientId").asText();

        // Alice hears about Bob joining.
        final JsonNode joined = awaitEvent(aliceToken,
                e -> "CLIENT_JOINED".equals(e.get("type").asText()) && bobId.equals(e.get("clientId").asText()));
        assertTrue(TIMESTAMP.matcher(joined.get("when").asText()).matches(), joined.toString());
        awaitEvent(aliceToken, e -> "CLIENT_STATUS".equals(e.get("type").asText()) && e.get("active").asInt() == 2);

        // Bob sends a message: escaped, with its link turned into an anchor.
        final HttpResponse<String> sent = post("/api/chat/message/" + bobToken + ".json",
                Map.of("message", "<b>hi</b> see http://example.com/?a=1&b=2 now"));
        assertEquals(200, sent.statusCode(), sent.body());
        final JsonNode message = MAPPER.readTree(sent.body());
        assertEquals("MESSAGE", message.get("type").asText());
        assertEquals(bobId, message.get("from").asText());
        assertEquals("&lt;b&gt;hi&lt;/b&gt; see <a href=\"http://example.com/?a=1&amp;b=2\" target=\"_blank\">"
                + "http://example.com/?a=1&amp;b=2</a> now", message.get("html").asText());

        final String messageId = message.get("id").asText();
        awaitEvent(aliceToken, e -> messageId.equals(e.get("id").asText()));
        awaitLog(aliceToken, log -> log.size() == 1 && messageId.equals(log.get(0).get("id").asText()));

        // Bob deletes it.
        final HttpResponse<String> deleted = delete("/api/chat/message/" + bobToken + ".json",
                Map.of("messageId", messageId));
        assertEquals(200, deleted.statusCode(), deleted.body());
        awaitEvent(aliceToken, e -> "DELETE".equals(e.get("type").asText())
                && messageId.equals(e.get("delete").asText()));
        awaitLog(aliceToken, JsonNode::isEmpty);

        // Typing and inactivity.
        final HttpResponse<String> typing = post("/api/chat/typing/" + bobToken + ".json", Map.of("typing", "true"));
        assertEquals(200, typing.statusCode(), typing.body());
        awaitEvent(aliceToken, e -> "TYPING".equals(e.get("type").asText())
                && e.get("typing").size() == 1 && bobId.equals(e.get("typing").get(0).asText()));
        final HttpResponse<String> inactive = post("/api/chat/inactivity/" + bobToken + ".json",
                Map.of("active", "false"));
        assertEquals(200, inactive.statusCode(), inactive.body());
        awaitEvent(aliceToken, e -> "CLIENT_STATUS".equals(e.get("type").asText())
                && e.get("inactive").asInt() == 1);
    }

    @Test
    public void idlePollReturnsNoOp() throws Exception {
        final String roomToken = createRoom("quiet-room").get("token").asText();
        final String token = register(roomToken).get("token").asText();

        // Drain the client's own join events, then wait out the (1s, in tests) long poll.
        awaitEvent(token, e -> "NOOP".equals(e.get("type").asText()));
    }

    @Test
    public void idleClientsAreRemovedFromTheRoom() throws Exception {
        final String roomToken = createRoom("idle-room").get("token").asText();
        final String aliceToken = register(roomToken).get("token").asText();
        final JsonNode bob = register(roomToken);
        final String bobToken = bob.get("token").asText();
        final String bobId = bob.get("clientId").asText();
        awaitEvent(aliceToken, e -> "CLIENT_STATUS".equals(e.get("type").asText()) && e.get("active").asInt() == 2);

        // Bob stops polling; Alice keeps polling, and hears that Bob left, then the new count.
        final JsonNode left = awaitEvent(aliceToken,
                e -> "CLIENT_LEFT".equals(e.get("type").asText()) && bobId.equals(e.get("clientId").asText()));
        assertTrue(TIMESTAMP.matcher(left.get("when").asText()).matches(), left.toString());
        awaitEvent(aliceToken, e -> "CLIENT_STATUS".equals(e.get("type").asText()) && e.get("active").asInt() == 1);
        assertEquals(412, get("/api/chat/event/" + bobToken + ".json").statusCode());
    }

    @Test
    public void badTokensAndParametersAreRejected() throws Exception {
        final String roomToken = createRoom("strict-room").get("token").asText();
        final String clientToken = register(roomToken).get("token").asText();

        // Malformed, forged, and a room token where a client token belongs.
        assertEquals(400, get("/api/chat/log/not-a-token.json").statusCode());
        assertEquals(400, get("/api/chat/log/" + clientToken.replace('_', '-') + ".json").statusCode());
        assertEquals(400, get("/api/chat/log/" + roomToken + ".json").statusCode());
        assertEquals(400, post("/api/chat/register/" + clientToken + ".json", Map.of()).statusCode());

        assertEquals(400, post("/api/chat/message/" + clientToken + ".json", Map.of()).statusCode());
        assertEquals(400, post("/api/chat/typing/" + clientToken + ".json", Map.of("typing", "maybe")).statusCode());
        assertEquals(400, delete("/api/chat/message/" + clientToken + ".json",
                Map.of("messageId", "not-a-uuid")).statusCode());
    }

    @Test
    public void errorsDoNotIncludeStackTracesOutsideDevMode() throws Exception {
        final HttpResponse<String> response = get("/api/chat/log/not-a-token.json");

        assertEquals(400, response.statusCode());
        assertFalse(MAPPER.readTree(response.body()).has("stackTrace"), response.body());
    }

    // Helpers

    private JsonNode createRoom(
            final String name) throws Exception {
        final HttpResponse<String> response = post("/api/room/create.json", Map.of("name", name));
        assertEquals(200, response.statusCode(), response.body());
        return MAPPER.readTree(response.body());
    }

    private JsonNode register(
            final String roomToken) throws Exception {
        final HttpResponse<String> response = post("/api/chat/register/" + roomToken + ".json", Map.of());
        assertEquals(200, response.statusCode(), response.body());
        return MAPPER.readTree(response.body());
    }

    /**
     * Long polls for the client's events, as the room page does, until one matches.
     */
    private JsonNode awaitEvent(
            final String clientToken,
            final Predicate<JsonNode> matches) throws Exception {
        final long deadline = System.currentTimeMillis() + EVENT_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            final HttpResponse<String> response = get("/api/chat/event/" + clientToken + ".json");
            assertEquals(200, response.statusCode(), response.body());
            final JsonNode event = MAPPER.readTree(response.body());
            if (matches.test(event)) {
                return event;
            }
        }
        return fail("No matching event within " + EVENT_WAIT_MS + "ms.");
    }

    /**
     * Messages are logged asynchronously, so poll the log until it matches.
     */
    private void awaitLog(
            final String clientToken,
            final Predicate<JsonNode> matches) throws Exception {
        final long deadline = System.currentTimeMillis() + EVENT_WAIT_MS;
        JsonNode log = null;
        while (System.currentTimeMillis() < deadline) {
            final HttpResponse<String> response = get("/api/chat/log/" + clientToken + ".json");
            assertEquals(200, response.statusCode(), response.body());
            log = MAPPER.readTree(response.body()).get("log");
            if (matches.test(log)) {
                return;
            }
            Thread.sleep(50L);
        }
        fail("Chat log never matched: " + log);
    }

    private HttpResponse<String> get(
            final String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(baseUri_ + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(
            final String path,
            final Map<String, String> form) throws Exception {
        return send("POST", path, form);
    }

    private HttpResponse<String> delete(
            final String path,
            final Map<String, String> form) throws Exception {
        return send("DELETE", path, form);
    }

    /**
     * Sends a form encoded body, as jQuery does for the page's POST and DELETE requests.
     */
    private HttpResponse<String> send(
            final String method,
            final String path,
            final Map<String, String> form) throws Exception {
        final String body = form.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return HTTP.send(HttpRequest.newBuilder(URI.create(baseUri_ + path))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String encode(
            final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
