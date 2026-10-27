package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.storage.SqliteStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpaTrustStoreTest {

    private final UUID truster = UUID.randomUUID();
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    private SqliteStorage storage;
    private TpaTrustStore trusts;

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("trust-test"));
        assertTrue(storage.init());
        trusts = new TpaTrustStore(() -> storage);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @Test
    @DisplayName("should trust and untrust one player")
    void shouldToggleTrust() {
        assertFalse(trusts.isTrusted(truster, first));

        trusts.setTrusted(truster, first, true);

        assertTrue(trusts.isTrusted(truster, first));

        trusts.setTrusted(truster, first, false);

        assertFalse(trusts.isTrusted(truster, first));
    }

    @Test
    @DisplayName("should list trusted players oldest first")
    void shouldListTrusted() {
        assertEquals(List.of(), trusts.trusted(truster));

        trusts.setTrusted(truster, first, true);
        trusts.setTrusted(truster, second, true);

        assertEquals(List.of(first, second), trusts.trusted(truster));

        trusts.setTrusted(truster, first, false);

        assertEquals(List.of(second), trusts.trusted(truster));
    }
}
