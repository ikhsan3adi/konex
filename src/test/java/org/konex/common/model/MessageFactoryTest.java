package org.konex.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageFactoryTest {

    @Test
    @DisplayName("Factory harus membuat TextMessage yang valid")
    void testCreateTextMessage() {
        // Arrange
        User sender = new User("u1", "Budi", "08123", null, "pass");
        String chatId = "global_room";
        String content = "Halo Dunia";

        // Act
        Message result = MessageFactory.createMessage(chatId, sender, content);

        // Assert
        assertNotNull(result, "Message tidak boleh null");
        assertTrue(result instanceof TextMessage, "Objek harus berupa instance TextMessage");
        assertEquals("TEXT", result.getType(), "Tipe pesan harus TEXT");
        assertEquals(content, result.getContent(), "Isi pesan harus sesuai");
        assertEquals(chatId, result.getChatId(), "Chat ID harus sesuai");
        assertEquals(sender, result.getSender(), "Pengirim harus sesuai");
    }

    @Test
    @DisplayName("Factory harus membuat ImageMessage yang valid")
    void testCreateImageMessage() {
        // Arrange
        User sender = new User("u2", "Andi", "08111", null, "pass");
        String chatId = "private_room";
        String caption = "Liburan";
        String base64Dummy = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...";

        // Act
        Message result = MessageFactory.createMessage(chatId, sender, caption, base64Dummy);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ImageMessage, "Objek harus berupa instance ImageMessage");
        assertEquals("IMAGE", result.getType(), "Tipe pesan harus IMAGE");
        assertEquals(caption, result.getContent(), "Content harus berisi caption");

        // Cek data spesifik ImageMessage
        ImageMessage imgMsg = (ImageMessage) result;
        assertEquals(base64Dummy, imgMsg.getBase64Data(), "Data Base64 gambar harus tersimpan");
    }
}