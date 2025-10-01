package com.example.bankcards.service.transfer;


import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;

public interface TransferService {
    TransferResponse transfer(Long userId, TransferRequest req);
}