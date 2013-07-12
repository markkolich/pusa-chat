package com.kolich.pusachat.spring.controllers.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.kolich.common.util.secure.KolichStringSigner;
import com.kolich.pusachat.spring.beans.ChatRooms;
import com.kolich.pusachat.spring.controllers.AbstractPusaChatController;
import com.kolich.pusachat.spring.controllers.PusaChatControllerClosure;
import com.kolich.spring.beans.KolichWebAppProperties;

@Controller
@RequestMapping(value="/")
public class Index extends AbstractPusaChatController {
	
	private static final Logger logger__ = LoggerFactory.getLogger(Index.class);
	
	private static final String VIEW_NAME = "index";
	
	@Autowired
	public Index(KolichWebAppProperties properties,
		KolichStringSigner signer,
		ChatRooms rooms) {
		super(logger__, properties, signer, rooms);
	}

	@RequestMapping(method={RequestMethod.GET, RequestMethod.HEAD})
	public ModelAndView index(@RequestParam(required=false) final String key) {
		return new PusaChatControllerClosure<ModelAndView>(
			"GET:/", logger__) {
			@Override
			public ModelAndView doit() throws Exception {
				final ModelAndView mav = getModelAndView(VIEW_NAME);
				mav.addObject("key", key);
				return mav;
			}
		}.execute();
	}
	
}
