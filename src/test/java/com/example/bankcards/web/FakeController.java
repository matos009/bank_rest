package com.example.bankcards.web;

import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FakeController {
    @GetMapping("/t/404") String n() { throw new NotFoundException("no such thing"); }
    @GetMapping("/t/409") String c() { throw new ConflictException("dup"); }
    @GetMapping("/t/400") String b() { throw new BusinessException("bad input"); }
    @GetMapping("/t/500") String e() { throw new RuntimeException("boom"); }
}