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

package pusachat.controllers;

import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.RequestBody;
import pusachat.components.chat.ChatSessions;
import pusachat.entities.ChatLog;
import pusachat.entities.PusaChatSession;
import pusachat.entities.events.ClientStatus;
import pusachat.entities.events.Delete;
import pusachat.entities.events.Message;
import pusachat.entities.events.PusaChatEvent;
import pusachat.entities.events.Typing;

import static curacao.annotations.RequestMapping.Method.DELETE;
import static curacao.annotations.RequestMapping.Method.GET;
import static curacao.annotations.RequestMapping.Method.POST;

/**
 * The chat API a room's page talks to. A client registers with the room token, then uses the
 * client token it gets back for everything else. Events (messages, people joining, typing)
 * are delivered by long polling: the page keeps one request to {@code /event} open at a time.
 */
@Controller
public final class Chat extends AbstractPusaChatController {

    private final ChatSessions sessions_;

    @Injectable
    public Chat(
            final ChatSessions sessions) {
        sessions_ = sessions;
    }

    @RequestMapping(value = "^/api/chat/register/" + TOKEN_REGEX + "\\.json$",
            methods = POST)
    public PusaChatSession register(
            @Path("token") final String roomToken) {
        return sessions_.register(roomToken);
    }

    /**
     * Waits for the client's next event, returning a NOOP if nothing happens within the long
     * poll timeout. Either way, the client then asks again, so this doubles as its keep-alive.
     */
    @RequestMapping(value = "^/api/chat/event/" + TOKEN_REGEX + "\\.json$",
            methods = GET)
    public PusaChatEvent event(
            @Path("token") final String token) throws InterruptedException {
        return sessions_.nextEvent(sessions_.getSession(token));
    }

    @RequestMapping(value = "^/api/chat/log/" + TOKEN_REGEX + "\\.json$",
            methods = GET)
    public ChatLog log(
            @Path("token") final String token) {
        return sessions_.getChatLog(sessions_.getSession(token));
    }

    @RequestMapping(value = "^/api/chat/message/" + TOKEN_REGEX + "\\.json$",
            methods = POST)
    public Message message(
            @Path("token") final String token,
            @RequestBody("message") final String message) {
        return sessions_.postMessage(sessions_.getSession(token), requireParam("message", message));
    }

    @RequestMapping(value = "^/api/chat/message/" + TOKEN_REGEX + "\\.json$",
            methods = DELETE)
    public Delete deleteMessage(
            @Path("token") final String token,
            @RequestBody("messageId") final String messageId) {
        return sessions_.deleteMessage(sessions_.getSession(token), requireUuidParam("messageId", messageId));
    }

    @RequestMapping(value = "^/api/chat/typing/" + TOKEN_REGEX + "\\.json$",
            methods = POST)
    public Typing typing(
            @Path("token") final String token,
            @RequestBody("typing") final String typing) {
        return sessions_.setTyping(sessions_.getSession(token), requireBooleanParam("typing", typing));
    }

    @RequestMapping(value = "^/api/chat/inactivity/" + TOKEN_REGEX + "\\.json$",
            methods = POST)
    public ClientStatus inactivity(
            @Path("token") final String token,
            @RequestBody("active") final String active) {
        return sessions_.setActive(sessions_.getSession(token), requireBooleanParam("active", active));
    }

}
