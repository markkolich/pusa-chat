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
import com.kolich.spring.beans.KolichWebAppProperties;

@Controller
@RequestMapping(value="/api/room")
public class Room extends AbstractPusaChatController {
	
	private static final Logger logger__ = LoggerFactory.getLogger(Room.class);
	
	private static final String VIEW_NAME = "room";
	
	private static final int DEFAULT_RANDOM_ROOM_NAME_LENGTH = 10;
	
	@Autowired
	public Room(KolichWebAppProperties properties,
		KolichStringSigner signer,
		ChatRooms rooms) {
		super(logger__, properties, signer, rooms);
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
