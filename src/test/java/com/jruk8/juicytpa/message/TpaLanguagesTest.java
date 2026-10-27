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
        assertEquals("zh_tw", TpaLanguages.normalize("zh_tw"));
        assertEquals("zh_tw", TpaLanguages.normalize("zh-Hant-HK"));
        assertEquals("zh_cn", TpaLanguages.normalize("zh_cn"));
        assertEquals("zh_cn", TpaLanguages.normalize("zh"));
        assertEquals("pt_br", TpaLanguages.normalize("pt_br"));
        assertEquals("pt_br", TpaLanguages.normalize("pt_ao"));
        assertEquals("pt_pt", TpaLanguages.normalize("pt_pt"));
        assertEquals("pt_br", TpaLanguages.normalize("pt"));
        assertEquals("sv", TpaLanguages.normalize("sv_se"));
        assertEquals("en", TpaLanguages.normalize("se_no"));
        assertEquals("lol_us", TpaLanguages.normalize("lol_us"));
        assertEquals("en_pt", TpaLanguages.normalize("en_pt"));
        assertEquals("qya_aa", TpaLanguages.normalize("qya_aa"));
        assertEquals("he", TpaLanguages.normalize("iw_il"));
        assertEquals("id", TpaLanguages.normalize("in_id"));
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
        assertEquals("en", TpaLanguages.normalize("sw_ke"));
    }

    @Test
    @DisplayName("should match codes, subtags and names case-insensitively")
    void shouldMatchInput() {
        assertEquals(Optional.of("fi"), TpaLanguages.matches("fi"));
        assertEquals(Optional.of("fi"), TpaLanguages.matches(" FI "));
        assertEquals(Optional.of("sv"), TpaLanguages.matches("sv"));
        assertEquals(Optional.of("sv"), TpaLanguages.matches("se"));
        assertEquals(Optional.of("pt_br"), TpaLanguages.matches("pt-br"));
        assertEquals(Optional.of("zh_tw"), TpaLanguages.matches("zh-tw"));
        assertEquals(Optional.of("pt_br"), TpaLanguages.matches("portuguese"));
        assertEquals(Optional.of("zh_cn"), TpaLanguages.matches("chinese"));
        assertEquals(Optional.of("lol_us"), TpaLanguages.matches("lolcat"));
        assertEquals(Optional.of("en_pt"), TpaLanguages.matches("pirate speak"));
        assertEquals(Optional.of("qya_aa"), TpaLanguages.matches("quENya"));
        assertEquals(Optional.of("no"), TpaLanguages.matches("nb"));
        assertEquals(Optional.of("fi"), TpaLanguages.matches("finnish"));
        assertEquals(Optional.of("fi"), TpaLanguages.matches("Suomi"));
        assertEquals(Optional.of("de"), TpaLanguages.matches("DEUTSCH"));
        assertEquals(Optional.of("zh_cn"), TpaLanguages.matches("中文"));
        assertEquals(Optional.of("zh_tw"), TpaLanguages.matches("繁體中文"));
        assertTrue(TpaLanguages.matches("klingon").isEmpty());
        assertTrue(TpaLanguages.matches(null).isEmpty());
        assertTrue(TpaLanguages.matches("  ").isEmpty());
    }

    @Test
    @DisplayName("should label every supported code")
    void shouldLabelCodes() {
        assertEquals("Suomi (FI)", TpaLanguages.label("fi"));
        assertEquals("English (EN)", TpaLanguages.label("en"));
        assertEquals("Svenska (SV)", TpaLanguages.label("sv"));
        assertEquals("Português (PT_BR)", TpaLanguages.label("pt_br"));
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
