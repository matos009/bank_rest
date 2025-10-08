package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.enums.TransferStatus;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.service.transfer.TransferService;
import com.example.bankcards.web.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransferController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean TransferService transfers;

    private TransferResponse sample() {
        return new TransferResponse(
                99L, 7L, 11L, 22L,
                new BigDecimal("50.00"),
                TransferStatus.SUCCESS,
                "test move",
                OffsetDateTime.parse("2025-01-01T10:00:00Z")
        );
    }

    @Test
    @WithMockUser(username = "7", roles = "USER")
    void transfer_created_201() throws Exception {
        var req  = new TransferRequest(11L, 22L, new BigDecimal("50.00"), "test move");
        var resp = sample();

        given(transfers.transferForCurrentUser(any(TransferRequest.class))).willReturn(resp);

        mvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.fromCardId").value(11))
                .andExpect(jsonPath("$.toCardId").value(22))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.description").value("test move"));
    }

    @Test
    @WithMockUser(username = "7", roles = "USER")
    void transfer_bad_request_400_from_service() throws Exception {
        var req = new TransferRequest(11L, 22L, new BigDecimal("0.00"), "oops");

        given(transfers.transferForCurrentUser(any(TransferRequest.class)))
                .willThrow(new BusinessException("Amount must be > 0"));

        mvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Amount must be > 0"));
    }
}