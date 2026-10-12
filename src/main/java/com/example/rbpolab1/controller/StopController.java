package com.example.rbpolab1.controller;

import com.example.rbpolab1.model.Stop;
import com.example.rbpolab1.service.StopService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final StopService stopService;

    public StopController(StopService stopService) {
        this.stopService = stopService;
    }

    @GetMapping
    public List<Stop> findAll() {
        return stopService.findAll();
    }

    @GetMapping("/{id}")
    public Stop findById(@PathVariable("id") Long id) {
        return stopService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Stop> create(@RequestBody Stop request) {
        validateRequest(request);

        Stop stop = stopService.create(
                request.getRouteId(),
                request.getAddress(),
                request.getSequenceNumber()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(stop);
    }

    @PatchMapping("/{id}")
    public Stop update(@PathVariable("id") Long id,
                       @RequestBody Stop request) {
        validateRequest(request);

        return stopService.update(
                id,
                request.getRouteId(),
                request.getAddress(),
                request.getSequenceNumber()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        stopService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateRequest(Stop request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
    }
}