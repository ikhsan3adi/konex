package org.konex.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    @Test
    @DisplayName("Test Response Generic dengan tipe String")
    void testResponseWithString() {
        // Arrange
        String payload = "ROOMLIST:Global,Alumni";

        // Act
        Response<String> response = Response.success("ROOMLIST", payload);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("ROOMLIST", response.getCommand());
        assertEquals("OK", response.getMessage());

        // Cek Tipe Data
        assertNotNull(response.getData());
        assertEquals(payload, response.getData());
    }

    @Test
    @DisplayName("Test Response Generic dengan tipe User (Login Success)")
    void testResponseWithUser() {
        // Arrange
        User user = new UserBuilder().setName("Budi").setPhone("081").build();

        // Act
        Response<User> response = Response.success("LOGIN_SUCCESS", user);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Budi", response.getData().getName());
    }

    @Test
    @DisplayName("Test Response Error membawa pesan kesalahan")
    void testResponseError() {
        // Arrange
        String errorMessage = "Password Salah!";

        // Act

        Response<User> response = Response.error("LOGIN_FAILED", errorMessage);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("LOGIN_FAILED", response.getCommand());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData());
    }
}