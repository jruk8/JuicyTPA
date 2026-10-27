package com.jruk8.juicytpa.core.commands.tpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestPromptsTest {

    @Test
    @DisplayName("should link accept to tpaccept with a tooltip")
    void shouldBuildAccept() {
        assertEquals(
                "<click:run_command:'/tpaccept Steve'>"
                        + "<hover:show_text:'{ac}Click to accept {hl}Steve{ac}.'>"
                        + "{ac}[✔ Accept]</hover></click>",
                RequestPrompts.accept("Steve", "[✔ Accept]"));
    }

    @Test
    @DisplayName("should link reject to tpadeny with a tooltip")
    void shouldBuildReject() {
        assertEquals(
                "<click:run_command:'/tpadeny Steve'>"
                        + "<hover:show_text:'{rc}Click to reject {hl}Steve{rc}.'>"
                        + "{rc}[✕ Reject]</hover></click>",
                RequestPrompts.reject("Steve", "[✕ Reject]"));
    }

    @Test
    @DisplayName("should link block-all to tpamode with a tooltip")
    void shouldBuildBlockAll() {
        assertEquals(
                "<click:run_command:'/tpamode block'>"
                        + "<hover:show_text:'{lh}Click to block all requests.'>"
                        + "{lh}[💻 Block All]</hover></click>",
                RequestPrompts.blockAll("{lh}[💻 Block All]"));
    }

    @Test
    @DisplayName("should link block-player to tpablock with a tooltip")
    void shouldBuildBlockPlayer() {
        assertEquals(
                "<click:run_command:'/tpablock Steve'>"
                        + "<hover:show_text:'{lh}Click to block {hl}Steve{lh}.'>"
                        + "{lh}[👤 Block Steve]</hover></click>",
                RequestPrompts.blockPlayer("Steve", "{lh}[👤 Block Steve]"));
    }
}
