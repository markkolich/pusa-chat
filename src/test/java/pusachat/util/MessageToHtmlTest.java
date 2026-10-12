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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class MessageToHtmlTest {

    @Test
    public void plainTextIsUnchanged() {
        assertEquals("hello &lt;b&gt;world&lt;/b&gt;", MessageToHtml.makeHyperlinks("hello &lt;b&gt;world&lt;/b&gt;"));
    }

    @Test
    public void linksBecomeAnchors() {
        assertEquals("see <a href=\"https://example.com/a\" target=\"_blank\">https://example.com/a</a> and "
                + "<a href=\"http://example.org\" target=\"_blank\">http://example.org</a>",
                MessageToHtml.makeHyperlinks("see https://example.com/a and http://example.org"));
    }

    /**
     * Links with regex metacharacters ({@code ?}, {@code +}, {@code |}) are linked too; they used
     * to be turned into regexes and silently left as text.
     */
    @Test
    public void linksWithRegexCharactersBecomeAnchors() {
        final String url = "http://example.com/search?q=a+b&amp;in=x|y";

        assertEquals("<a href=\"" + url + "\" target=\"_blank\">" + url + "</a>",
                MessageToHtml.makeHyperlinks(url));
    }

    @Test
    public void closingParenthesisAfterLinkIsNotPartOfIt() {
        assertEquals("(see <a href=\"http://example.com\" target=\"_blank\">http://example.com</a>)",
                MessageToHtml.makeHyperlinks("(see http://example.com)"));
    }

    @Test
    public void onlyHttpLinksAreLinked() {
        assertEquals("www.example.com javascript:alert(1)",
                MessageToHtml.makeHyperlinks("www.example.com javascript:alert(1)"));
    }

}
