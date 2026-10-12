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

import org.junit.jupiter.api.Test;
import pusachat.exceptions.BadChatTokenException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TokenSignerTest {

    private final TokenSigner signer_ = new TokenSigner();

    @Test
    public void signedPayloadVerifies() {
        final String payload = UUID.randomUUID() + "_" + UUID.randomUUID();

        final String token = signer_.sign(payload);

        assertTrue(token.matches("[a-zA-Z0-9\\-_]+"), token);
        assertEquals(payload, signer_.verify(token));
    }

    @Test
    public void tamperedTokensDoNotVerify() {
        final String token = signer_.sign(UUID.randomUUID().toString());
        final String otherPayload = UUID.randomUUID().toString();

        // Another payload with the original signature.
        assertThrows(BadChatTokenException.class,
                () -> signer_.verify(otherPayload + token.substring(token.lastIndexOf('_'))));
        // A changed signature.
        assertThrows(BadChatTokenException.class, () -> signer_.verify(token + "0"));
        // No signature at all.
        assertThrows(BadChatTokenException.class, () -> signer_.verify(otherPayload));
        assertThrows(BadChatTokenException.class, () -> signer_.verify(null));
    }

    @Test
    public void tokensFromAnotherSignerDoNotVerify() {
        final String token = new TokenSigner().sign(UUID.randomUUID().toString());

        assertThrows(BadChatTokenException.class, () -> signer_.verify(token));
    }

}
