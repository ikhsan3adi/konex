package org.konex.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserBuilderTest {

    @Test
    @DisplayName("Test UserBuilder membuat objek User dengan atribut lengkap")
    void testBuildUserComplete() {
        // 1. Arrange
        String expectedName = "Budi Santoso";
        String expectedPhone = "08123456789";
        String expectedPassword = "rahasia123";
        String expectedImage = "base64stringdummy";

        // 2. Act
        User user = new UserBuilder()
                .setName(expectedName)
                .setPhone(expectedPhone)
                .setPassword(expectedPassword)
                .setProfileImage(expectedImage)
                .build();

        // 3. Assert
        assertNotNull(user, "Objek User tidak boleh null");
        assertEquals(expectedName, user.getName(), "Nama user harus sesuai");
        assertEquals(expectedPhone, user.getPhoneNumber(), "Nomor HP harus sesuai");
        assertEquals(expectedPassword, user.getPassword(), "Password harus sesuai");
        assertEquals(expectedImage, user.getProfileImage(), "Profile image harus sesuai");
    }

    @Test
    @DisplayName("Test UserBuilder menangani chaining method dengan benar")
    void testBuilderChaining() {
        UserBuilder builder = new UserBuilder();

        UserBuilder nextBuilder = builder.setName("Andi");

        assertSame(builder, nextBuilder, "Builder harus mengembalikan instance dirinya sendiri untuk chaining");
    }

    @Test
    @DisplayName("Test UserBuilder dengan data parsial")
    void testBuildUserPartial() {
        User user = new UserBuilder()
                .setName("Citra")
                .setPhone("08111")
                .build();

        assertEquals("Citra", user.getName());
        assertEquals("08111", user.getPhoneNumber());
        assertNull(user.getPassword(), "Password harus null jika tidak diset");
        assertNull(user.getProfileImage(), "Image harus null jika tidak diset");
    }
}