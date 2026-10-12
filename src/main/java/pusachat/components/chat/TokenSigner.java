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

package pusachat.components.chat;

import com.kolich.security.sign.HmacSigner;
import curacao.annotations.Component;
import pusachat.exceptions.BadChatTokenException;

import java.security.SecureRandom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Signs room and client tokens so clients can't forge them, with HMAC-SHA256 and a random key
 * made at startup. Rooms only live in memory, so tokens dying with a restart costs nothing.
 *
 * <p>A token is {@code <payload>_<hex signature>}. Payloads are UUIDs joined by {@code _}, so a
 * token only ever contains {@code [a-zA-Z0-9-_]} and is safe in a URL path as-is.
 *
 * <p>The HMAC itself comes from {@link HmacSigner}, whose {@code verify} compares in constant
 * time so the signature can't be discovered by timing the comparison.
 */
@Component
public final class TokenSigner {

    private static final String SIGNATURE_DELIMITER = "_";

    private static final int KEY_BYTES = 32;

    private final byte[] key_;

    public TokenSigner() {
        key_ = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(key_);
    }

    public String sign(
            final String payload) {
        checkNotNull(payload, "Payload to sign cannot be null.");
        checkArgument(payload.matches("[a-zA-Z0-9\\-_]+"), "Payload must be URL path safe: %s", payload);
        return payload + SIGNATURE_DELIMITER + signer(payload).sign();
    }

    /**
     * Returns the payload of a token this signer signed.
     *
     * @throws BadChatTokenException if the token is malformed or wasn't signed by this signer
     */
    public String verify(
            final String token) {
        final int delimiter = (token == null) ? -1 : token.lastIndexOf(SIGNATURE_DELIMITER);
        if (delimiter <= 0) {
            throw new BadChatTokenException("Malformed token: " + token);
        }

        final String payload = token.substring(0, delimiter);
        if (!signer(payload).verify(token.substring(delimiter + 1))) {
            throw new BadChatTokenException("Token signature does not match: " + token);
        }

        return payload;
    }

    private HmacSigner signer(
            final String payload) {
        return new HmacSigner.Builder()
                .setAlgorithm(HmacSigner.Algorithm.HMAC_SHA256)
                .setEncoding(HmacSigner.Encoding.HEX)
                .setSecret(key_)
                .setMessage(payload)
                .build();
    }

}
