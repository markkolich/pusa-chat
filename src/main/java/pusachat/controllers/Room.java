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
import curacao.annotations.Route;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.RequestBody;
import curacao.freemarker.entities.FreeMarkerContent;
import org.apache.commons.lang3.RandomStringUtils;
import pusachat.components.chat.ChatRooms;
import pusachat.components.freemarker.PusaChatFreeMarkerContent;
import pusachat.entities.ChatRoom;

import java.util.Locale;

import static curacao.annotations.Route.Method.GET;
import static curacao.annotations.Route.Method.POST;

/**
 * Rooms: the chat page for a room, and finding or creating a room by name.
 */
@Controller
public final class Room extends AbstractPusaChatController {

    private static final int RANDOM_ROOM_NAME_LENGTH = 10;

    private final ChatRooms rooms_;

    @Injectable
    public Room(
            final ChatRooms rooms) {
        rooms_ = rooms;
    }

    /**
     * The chat page for the room a room token was issued for.
     */
    @Route(value = "^/api/room/" + TOKEN_REGEX + "$",
            methods = GET)
    public FreeMarkerContent show(
            @Path("token") final String token) {
        final ChatRoom room = rooms_.getRoomFromToken(token);
        return PusaChatFreeMarkerContent.newHtmlBuilder("templates/room.ftl")
                // The page's "id" is the room token, which the page registers with.
                .withAttr("id", room.getToken())
                .withAttr("name", room.getName())
                .build();
    }

    @Route(value = "^/api/room/join\\.json$",
            methods = POST)
    public ChatRoom join(
            @RequestBody("name") final String name) {
        // Doesn't create the room if it doesn't exist.
        return rooms_.getRoomByName(requireParam("name", name).toLowerCase(Locale.ROOT), false);
    }

    @Route(value = "^/api/room/create\\.json$",
            methods = POST)
    public ChatRoom create(
            @RequestBody("name") final String name) {
        return rooms_.getRoomByName(requireParam("name", name).toLowerCase(Locale.ROOT), true);
    }

    @Route(value = "^/api/room/random\\.json$",
            methods = GET)
    public ChatRoom random() {
        final String name = RandomStringUtils.secure().nextAlphanumeric(RANDOM_ROOM_NAME_LENGTH);
        return rooms_.getRoomByName(name.toLowerCase(Locale.ROOT), true);
    }

}
