package com.jruk8.juicytpa.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpaLanguagesTest {

    @Test
    @DisplayName("should map client locales onto codes")
    void shouldNormalizeLocales() {
        assertEquals("en", TpaLanguages.normalize("en_us"));
        assertEquals("fi", TpaLanguages.normalize("fi_fi"));
        assertEquals("de", TpaLanguages.normalize("DE_de"));
        assertEquals("zh", TpaLanguages.normalize("zh_tw"));
        assertEquals("pt", TpaLanguages.normalize("pt_br"));
        assertEquals("se", TpaLanguages.normalize("sv_se"));
        assertEquals("no", TpaLanguages.normalize("nb_no"));
        assertEquals("no", TpaLanguages.normalize("nn_no"));
    }

    @Test
    @DisplayName("should fall back to English on unknown locales")
    void shouldFallBackToEnglish() {
        assertEquals("en", TpaLanguages.normalize(null));
        assertEquals("en", TpaLanguages.normalize(""));
        assertEquals("en", TpaLanguages.normalize("  "));
        assertEquals("en", TpaLanguages.normalize("xx_yy"));
        assertEquals("en", TpaLanguages.normalize("he_il"));
    }

    @Test
    @DisplayName("should match codes, subtags and names case-insensitively")
    void shouldMatchInput() {
        assertEquals(Optional.of("fi"), TpaLanguages.matches("fi"));
        assertEquals(Optional.of("fi"), TpaLanguages.matches(" FI "));
        assertEquals(Optional.of("se"), TpaLanguages.matches("sv"));
        assertEquals(Optional.of("no"), TpaLanguages.matches("nb"));
        assertEquals(Optional.of("fi"), TpaLanguages.matches("finnish"));
        assertEquals(Optional.of("fi"), TpaLanguages.matches("Suomi"));
        assertEquals(Optional.of("de"), TpaLanguages.matches("DEUTSCH"));
        assertEquals(Optional.of("zh"), TpaLanguages.matches("中文"));
        assertTrue(TpaLanguages.matches("klingon").isEmpty());
        assertTrue(TpaLanguages.matches(null).isEmpty());
        assertTrue(TpaLanguages.matches("  ").isEmpty());
    }

    @Test
    @DisplayName("should label every supported code")
    void shouldLabelCodes() {
        assertEquals("Suomi (FI)", TpaLanguages.label("fi"));
        assertEquals("English (EN)", TpaLanguages.label("en"));
        assertEquals("Svenska (SE)", TpaLanguages.label("se"));
        for (String code : TpaLanguages.SUPPORTED) {
            assertFalse(TpaLanguages.nativeName(code).isBlank());
            assertTrue(TpaLanguages.isSupported(code));
        }
        assertFalse(TpaLanguages.isSupported("xx"));
    }

    @Test
    @DisplayName("should round-trip storage ids")
    void shouldRoundTripIds() {
        for (String code : TpaLanguages.SUPPORTED) {
            assertEquals(Optional.of(code), TpaLanguages.codeOf(TpaLanguages.idOf(code)));
        }
        assertTrue(TpaLanguages.codeOf(-1).isEmpty());
        assertTrue(TpaLanguages.codeOf(99).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> TpaLanguages.idOf("xx"));
    }
}
