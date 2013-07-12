package com.kolich.pusachat.spring.controllers.api;

import static com.kolich.pusachat.entities.events.Delete.MESSAGE_ID_DELETE_QUERY_PARAM;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.kolich.common.util.secure.KolichStringSigner;
import com.kolich.pusachat.entities.PusaChatSession;
import com.kolich.pusachat.entities.events.NoOp;
import com.kolich.pusachat.entities.events.PusaChatEvent;
import com.kolich.pusachat.spring.beans.ChatRooms;
import com.kolich.pusachat.spring.controllers.AbstractPusaChatController;
import com.kolich.spring.beans.KolichWebAppProperties;
import com.kolich.spring.controllers.KolichControllerClosure;

@Controller
@RequestMapping(value="/api/chat")
public class Chat extends AbstractPusaChatController {
	
	private static final Logger logger__ = LoggerFactory.getLogger(Chat.class);
	
	private static final String VIEW_NAME = "chat";
	
	private static final int DEFAULT_BOSH_WAIT_IN_SECONDS = 20;
		
	@Autowired
	public Chat(KolichWebAppProperties properties,
		KolichStringSigner signer,
		ChatRooms rooms) {
		super(logger__, properties, signer, rooms);
	}
	
	@RequestMapping(method={RequestMethod.POST},
		value="/register/{roomToken}")
	public ModelAndView register(@PathVariable final String roomToken) {
		return new KolichControllerClosure<ModelAndView>(
			"POST:/api/chat/register/" + roomToken, logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				final PusaChatSession session = registerClient(
					getRoomIdFromToken(roomToken));
				logger__.debug("Client registration successful (token=" +
					session.getToken() + ", clientId=" + session.getClientId() +
						", roomToken=" + roomToken + ")");
				return getModelAndView(VIEW_NAME, session);
			}
		}.execute();
	}
	
	@RequestMapping(method={RequestMethod.GET, RequestMethod.HEAD},
		value="/event/{token}")
	public ModelAndView event(@PathVariable final String token) {
		return new KolichControllerClosure<ModelAndView>(
			"GET:/api/chat/event/" + token, logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				final PusaChatSession session = getSession(token);
				// NOTE: We used to wait here forever, then we realized that we
				// can use the poll() method to only wait up-to a certain amount
				// of time. This works great becuase now we don't need to waste
				// resources on a special "ping" method.  We simply have the
				// client wait for 10-seconds then return a NOOP if no events
				// are to be delivered to the waiting client within that
				// 10-second interval. Upon receiving the NOOP, the client
				// should immeaditely kick off another BOSH transaction which
				// calls this method again, hence acting like a "ping".
				//final PusaChatEvent event = session.getEventQueue().take();
				final PusaChatEvent event = session.getEventQueue().poll(
					// Wait at least this long for a new event to fall into
					// the queue.
					DEFAULT_BOSH_WAIT_IN_SECONDS,
					// Seconds.
					SECONDS);
				return getModelAndView(VIEW_NAME,
					// The poll() method may expire before there are any events
					// worth sending to the client.
					(event != null) ?
						// Real event.
						event :
						// Nothing, event was null, so just return a NOOP.
						new NoOp());
			}
		}.execute();
	}
	
	/*
	@RequestMapping(method={RequestMethod.GET, RequestMethod.HEAD},
		value="/log/{token}")
	public ModelAndView log(@PathVariable final String token) {
		return new PusaChatControllerClosure<ModelAndView>(
			"GET:/api/chat/log/" + token, logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				final PusaChatSession session = getSession(token);
				return getModelAndView(VIEW_NAME,
					getChatLog(session.getRoomId()));
			}
		}.execute();
	}
	*/
	
	@RequestMapping(method={RequestMethod.POST},
		value="/message/{token}")
	public ModelAndView message(@PathVariable final String token,
		@RequestParam(required=true) final String message) {
		return new KolichControllerClosure<ModelAndView>(
			"POST:/api/chat/message/" + token, logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				final PusaChatSession session = getSession(token);
				return getModelAndView(VIEW_NAME,
					postMessage(session.getRoomId(),
						session.getClientId(),
						message));
			}
		}.execute();
	}
	
	@RequestMapping(method={RequestMethod.DELETE},
		value="/message/{token}")
	public ModelAndView deleteMessage(@PathVariable final String token,
		@RequestBody final MultiValueMap<String, String> params) {
		return new KolichControllerClosure<ModelAndView>(
			"DELETE:/api/chat/message/" + token, logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				final PusaChatSession session = getSession(token);
				return getModelAndView(VIEW_NAME,
					postDeleteMessage(session.getRoomId(),
						session.getClientId(),
						params.getFirst(MESSAGE_ID_DELETE_QUERY_PARAM)));
			}
		}.execute();
	}
	
	@RequestMapping(method={RequestMethod.POST},
		value="/typing/{token}")
	public ModelAndView typing(@PathVariable final String token,
		@RequestParam(required=true) final boolean typing) {
		return new KolichControllerClosure<ModelAndView>(
			"POST:/api/chat/typing/" + token + "?typing=" + typing, logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				final PusaChatSession session = getSession(token);
				return getModelAndView(VIEW_NAME,
					setTypingStatus(session.getRoomId(),
						session.getClientId(),
						typing));
			}
		}.execute();
	}
	
	@RequestMapping(method={RequestMethod.POST},
		value="/inactivity/{token}")
	public ModelAndView inactivity(@PathVariable final String token,
		@RequestParam(required=true) final boolean active) {
		return new KolichControllerClosure<ModelAndView>(
			"POST:/api/chat/inactivity/" + token + "?active=" + active, logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				final PusaChatSession session = getSession(token);
				return getModelAndView(VIEW_NAME,
					setInactivityStatus(session.getRoomId(),
						session.getClientId(),
						active));
			}
		}.execute();
	}
	
}
