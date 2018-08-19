package com.n26.controllers;

import com.n26.data.Stats;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = "/statistics",
                produces = MediaType.APPLICATION_JSON_VALUE)
public class Statistics {
    @GetMapping
    public ResponseEntity<?> getStatistics()
    {
        // TODO -- Implement me and remove dummy stats.
        Stats stats = new Stats(new BigDecimal(12345.678901),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                0L);
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }
}