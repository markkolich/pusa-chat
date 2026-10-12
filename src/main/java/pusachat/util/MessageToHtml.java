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

package pusachat.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the links in an already HTML escaped chat message into anchors.
 */
public final class MessageToHtml {

    // Based on http://www.codinghorror.com/blog/2008/10/the-problem-with-urls.html
    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\bhttps?://[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]",
            Pattern.CASE_INSENSITIVE);

    private MessageToHtml() {
    }

    /**
     * Wraps every http:// and https:// link in {@code escapedMessage} in an anchor that opens in a
     * new window. The message must already be HTML escaped, so a link can't break out of the
     * {@code href} attribute.
     */
    public static String makeHyperlinks(
            final String escapedMessage) {
        final Matcher matcher = URL_PATTERN.matcher(escapedMessage);
        final StringBuilder html = new StringBuilder();
        while (matcher.find()) {
            String url = matcher.group();
            String trailing = "";
            // A link in parentheses, "(see http://example.com)": the closing parenthesis is
            // part of the sentence, not the link.
            if (url.endsWith(")") && !url.contains("(")) {
                url = url.substring(0, url.length() - 1);
                trailing = ")";
            }
            matcher.appendReplacement(html, Matcher.quoteReplacement(
                    "<a href=\"" + url + "\" target=\"_blank\">" + url + "</a>" + trailing));
        }
        matcher.appendTail(html);
        return html.toString();
    }

}
