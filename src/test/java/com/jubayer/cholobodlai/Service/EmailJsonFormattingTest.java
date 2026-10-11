package com.jubayer.cholobodlai.Service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailJsonFormattingTest {
    @Test
    void safelyEscapesResendPayloadContent() {
        assertEquals("\"Hello \\\"World\\\"\\n\\\\\"", EmailDeliveryService.json("Hello \"World\"\n\\"));
        assertEquals("\"\"", EmailDeliveryService.json(null));
    }
}
