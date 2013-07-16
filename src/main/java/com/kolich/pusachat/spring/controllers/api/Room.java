/**
 * Copyright (c) 2013 Mark S. Kolich
 * http://mark.koli.ch
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

package com.kolich.pusachat.spring.controllers.api;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.kolich.common.util.secure.KolichStringSigner;
import com.kolich.pusachat.entities.ChatRoom;
import com.kolich.pusachat.spring.beans.ChatRooms;
import com.kolich.pusachat.spring.controllers.AbstractPusaChatController;
import com.kolich.pusachat.spring.controllers.PusaChatControllerClosure;

@Controller
@RequestMapping(value="/api/room")
public class Room extends AbstractPusaChatController {
	
	private static final Logger logger__ = LoggerFactory.getLogger(Room.class);
	
	private static final String VIEW_NAME = "room";
	
	private static final int DEFAULT_RANDOM_ROOM_NAME_LENGTH = 10;
	
	@Autowired
	public Room(KolichStringSigner signer,
		ChatRooms rooms) {
		super(logger__, signer, rooms);
	}
	
	@RequestMapping(method={RequestMethod.GET, RequestMethod.HEAD},
		value="{token}")
	public ModelAndView show(@PathVariable final String token,
		final HttpSession session) {
		return new PusaChatControllerClosure<ModelAndView>(
			"GET:/api/room/" + token, logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				final ChatRoom cr = getRoom(getRoomIdFromToken(token));
				final ModelAndView mav = getModelAndView(VIEW_NAME);
				// NOTE: The "id" of the room here is actually the
				// signed room UUID "token".
				mav.addObject("id", cr.getToken());
				mav.addObject("name", cr.getName());
				return mav;
			}
		}.execute();
	}
	
	@RequestMapping(method={RequestMethod.POST},
		value="join")
	public ModelAndView join(@RequestParam(required=true) final String name) {
		return new PusaChatControllerClosure<ModelAndView>(
			"GET:/api/room/join?name=" + name, logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				return getModelAndView(VIEW_NAME,
					// Don't create the room if it does not exist.
					getRoomFromKey(name.toLowerCase(), false));
			}
		}.execute();
	}
	
	@RequestMapping(method={RequestMethod.POST},
		value="create")
	public ModelAndView create(@RequestParam(required=true) final String name) {
		return new PusaChatControllerClosure<ModelAndView>(
			"GET:/api/room/create?name=" + name, logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				return getModelAndView(VIEW_NAME,
					getRoomFromKey(name.toLowerCase(), true));
			}
		}.execute();
	}
	
	@RequestMapping(method={RequestMethod.GET, RequestMethod.HEAD},
		value="random")
	public ModelAndView random() {
		return new PusaChatControllerClosure<ModelAndView>(
			"GET:/api/room/random", logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				return getModelAndView(VIEW_NAME,
					getRoomFromKey(randomAlphanumeric(
						DEFAULT_RANDOM_ROOM_NAME_LENGTH).toLowerCase(),
							true));
			}
		}.execute();
	}
		
}
