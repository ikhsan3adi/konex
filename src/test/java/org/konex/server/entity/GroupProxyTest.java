package org.konex.server.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.konex.common.model.User;
import org.konex.common.model.UserBuilder;

import static org.junit.jupiter.api.Assertions.*;

class GroupProxyTest {

    @Test
    @DisplayName("Proxy mengizinkan Admin untuk kick member")
    void testKickByAdmin() {
        // 1. Arrange (Siapkan Aktor)
        User admin = new UserBuilder().setName("Admin").setPhone("001").build();
        User victim = new UserBuilder().setName("Korban").setPhone("002").build();

        // Buat Grup Asli (Real Subject)
        GroupChat realGroup = new GroupChat("g1", "Group Test", admin);
        realGroup.inviteMember(victim);

        // Bungkus dengan Proxy
        GroupProxy proxy = new GroupProxy(realGroup);

        // 2. Act & Assert
        // Pastikan tidak ada error saat Admin melakukan aksi
        assertDoesNotThrow(() -> {
            proxy.kickMember(victim, admin);
        }, "Admin seharusnya memiliki akses untuk kick");

        // Verifikasi apakah member benar-benar terhapus di objek asli
        assertFalse(realGroup.isMember(victim), "Korban harusnya sudah hilang dari grup");
    }

    @Test
    @DisplayName("Proxy mencegah Member biasa untuk kick member lain")
    void testKickByNonAdmin() {
        // 1. Arrange
        User admin = new UserBuilder().setName("Admin").setPhone("001").build();
        User intruder = new UserBuilder().setName("Pengacau").setPhone("666").build(); // Bukan admin
        User victim = new UserBuilder().setName("Korban").setPhone("002").build();

        GroupChat realGroup = new GroupChat("g1", "Group Test", admin);
        realGroup.inviteMember(intruder);
        realGroup.inviteMember(victim);

        GroupProxy proxy = new GroupProxy(realGroup);

        // 2. Act & Assert
        // Harapan: Harus melempar SecurityException
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            proxy.kickMember(victim, intruder);
        });

        // Cek pesan errornya (Opsional, untuk memastikan errornya benar)
        System.out.println("Pesan Error Proxy: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("Only admin"), "Pesan error harus sesuai");

        // Verifikasi: Korban harus MASIH ADA di grup (Kick gagal)
        assertTrue(realGroup.isMember(victim), "Korban tidak boleh terhapus jika kick gagal");
    }
}