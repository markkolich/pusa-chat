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

import org.apache.commons.lang3.StringUtils;
import pusachat.exceptions.InvalidRequestException;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class AbstractPusaChatController {

    /**
     * A signed room or client token in a request path, captured as {@code token}. Tokens only
     * ever contain these characters (see {@link pusachat.components.chat.TokenSigner}).
     */
    protected static final String TOKEN_REGEX = "(?<token>[a-zA-Z0-9\\-_]{1,200})";

    protected static String requireParam(
            final String name,
            @Nullable final String value) {
        if (value == null) {
            throw new InvalidRequestException("Missing required parameter: " + name);
        }
        return value;
    }

    protected static boolean requireBooleanParam(
            final String name,
            @Nullable final String value) {
        final String bool = StringUtils.trimToEmpty(requireParam(name, value));
        if ("true".equalsIgnoreCase(bool)) {
            return true;
        } else if ("false".equalsIgnoreCase(bool)) {
            return false;
        }
        throw new InvalidRequestException("Parameter is not true or false: " + name + "=" + value);
    }

    protected static UUID requireUuidParam(
            final String name,
            @Nullable final String value) {
        try {
            return UUID.fromString(requireParam(name, value));
        } catch (final IllegalArgumentException e) {
            throw new InvalidRequestException("Parameter is not a UUID: " + name + "=" + value);
        }
    }

}
