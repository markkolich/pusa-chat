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

package com.kolich.pusachat.util;

import static java.util.regex.Matcher.quoteReplacement;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.gagawa.java.elements.A;

public class MessageToHtml {
	
	private static final String LINK_TARGET_BLANK = "_blank";
	
	// Precompile the URL extraction pattern for efficiency.
	// This pattern was borrowed from:
	// http://www.codinghorror.com/blog/2008/10/the-problem-with-urls.html
	private static final Pattern urlRegex__ =
		compile("\\(?\\b(https?://|www[.])" +
			"[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]",
				CASE_INSENSITIVE);
	
	public static final String makeHyperlinks(final String message) {
		String html = new String(message);
		Matcher m = null;
		for(final URL url : getUrlsInMessage(html)) {
			try {
				m = compile(url.toString(), CASE_INSENSITIVE).matcher(html);
				while(m.find()) {
					// Matcher.quoteReplacement() is necessary here to escape
					// $ signs (dollar signs) and other characters that are
					// interpretered differently by the regex parser.  In this
					// case, a dollar sign really means a "group" but we don't
					// want it to mean a group, so we use quoteReplacement()
					// to handle this situation.
					html = m.replaceAll(quoteReplacement(
						getUrlAnchor(url.toString())));
				}
			} catch (Exception e) {
				continue;
			}
		}
		return html;
	}
	
	private static final List<URL> getUrlsInMessage(final String message) {
		final List<URL> urls = new ArrayList<URL>();
		final Matcher m = urlRegex__.matcher(message);
		while(m.find()) {
			try {
				urls.add(new URL(m.group()));
			} catch (MalformedURLException e) {
				// Do nothing, if it's malformed just move on silently.
			}
		}
		return urls;
	}
	
	private static final String getUrlAnchor(final String href) {
		return new A().setHref(href).setTarget(LINK_TARGET_BLANK)
			.appendText(href).write();
	}
		
}