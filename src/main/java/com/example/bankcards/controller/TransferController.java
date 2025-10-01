package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.security.utils.SecurityUtils;
import com.example.bankcards.service.transfer.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transfers")
@RestController
@RequestMapping("/transfers")
@Validated
public class TransferController {

    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }


    @Operation(summary = "Create transfer between own cards (TEMP userId query till JWT)")
    @PostMapping
    public TransferResponse transfer(@RequestBody @Valid TransferRequest req) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException("Unauthenticated");
        }
        return transfers.transfer(userId, req);
    }
}