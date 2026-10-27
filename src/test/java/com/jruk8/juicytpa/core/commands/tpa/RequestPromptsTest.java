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
                        + "{ac}[Accept]</hover></click>",
                RequestPrompts.accept("Steve"));
    }

    @Test
    @DisplayName("should link reject to tpadeny with a tooltip")
    void shouldBuildReject() {
        assertEquals(
                "<click:run_command:'/tpadeny Steve'>"
                        + "<hover:show_text:'{rc}Click to reject {hl}Steve{rc}.'>"
                        + "{rc}[Reject]</hover></click>",
                RequestPrompts.reject("Steve"));
    }
}
