package com.fraudwatch.fraud.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.fraud.config.SecurityConfig;
import com.fraudwatch.fraud.dto.FraudDecisionResponse;
import com.fraudwatch.fraud.dto.FraudRuleResponse;
import com.fraudwatch.fraud.dto.UpdateFraudRuleRequest;
import com.fraudwatch.fraud.exception.FraudBusinessException;
import com.fraudwatch.fraud.exception.FraudExceptionHandler;
import com.fraudwatch.fraud.service.FraudDecisionQueryService;
import com.fraudwatch.fraud.service.FraudRuleService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FraudController.class)
@Import({SecurityConfig.class, FraudExceptionHandler.class})
class FraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FraudRuleService fraudRuleService;

    @MockBean
    private FraudDecisionQueryService fraudDecisionQueryService;

    @Test
    void shouldReturnRules() throws Exception {
        when(fraudRuleService.getRules()).thenReturn(List.of(
            new FraudRuleResponse(
                7L,
                "HIGH_RISK_ACCOUNT_BEHAVIOR",
                "High Risk Account Behavior",
                "Accumulates risky behavior signals over time",
                40,
                true
            )
        ));

        mockMvc.perform(get("/api/fraud/rules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].code").value("HIGH_RISK_ACCOUNT_BEHAVIOR"))
            .andExpect(jsonPath("$[0].weight").value(40))
            .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    void shouldUpdateRule() throws Exception {
        when(fraudRuleService.updateRule(eq(7L), any(UpdateFraudRuleRequest.class)))
            .thenReturn(new FraudRuleResponse(
                7L,
                "HIGH_RISK_ACCOUNT_BEHAVIOR",
                "High Risk Account Behavior",
                "Accumulates risky behavior signals over time",
                55,
                false
            ));

        mockMvc.perform(put("/api/fraud/rules/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateFraudRuleRequest(false, 55))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.weight").value(55))
            .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void shouldReturnDecisionByTransactionId() throws Exception {
        when(fraudDecisionQueryService.getDecision(101L)).thenReturn(new FraudDecisionResponse(
            17L,
            101L,
            "tx-101",
            5001L,
            82,
            "BLOCK",
            List.of("NEW_IP_DETECTION", "HIGH_RISK_ACCOUNT_BEHAVIOR"),
            List.of("IP address is new for account", "Multiple risky behavior signals accumulated"),
            Instant.parse("2026-06-18T14:00:00Z")
        ));

        mockMvc.perform(get("/api/fraud/decisions/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.transactionReference").value("tx-101"))
            .andExpect(jsonPath("$.decision").value("BLOCK"))
            .andExpect(jsonPath("$.triggeredRules[1]").value("HIGH_RISK_ACCOUNT_BEHAVIOR"));
    }

    @Test
    void shouldRejectInvalidRuleUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/fraud/rules/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateFraudRuleRequest(null, 120))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details.fields.enabled").exists())
            .andExpect(jsonPath("$.details.fields.weight").exists());
    }

    @Test
    void shouldTranslateNotFoundRuleError() throws Exception {
        when(fraudRuleService.updateRule(eq(999L), any(UpdateFraudRuleRequest.class)))
            .thenThrow(new FraudBusinessException(HttpStatus.NOT_FOUND, "Fraud rule was not found"));

        mockMvc.perform(put("/api/fraud/rules/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateFraudRuleRequest(true, 30))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Fraud rule was not found"))
            .andExpect(jsonPath("$.path").value("/api/fraud/rules/999"));
    }

    @Test
    void shouldTranslateNotFoundDecisionError() throws Exception {
        when(fraudDecisionQueryService.getDecision(404L))
            .thenThrow(new FraudBusinessException(HttpStatus.NOT_FOUND, "Fraud decision was not found"));

        mockMvc.perform(get("/api/fraud/decisions/404"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Fraud decision was not found"))
            .andExpect(jsonPath("$.path").value("/api/fraud/decisions/404"));
    }
}
