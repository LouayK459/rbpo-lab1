package com.example.rbpolab1.controller;

import com.example.rbpolab1.model.Route;
import com.example.rbpolab1.service.RouteService;
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
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<Route> findAll() {
        return routeService.findAll();
    }

    @GetMapping("/{id}")
    public Route findById(@PathVariable("id") Long id) {
        return routeService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Route> create(@RequestBody Route request) {
        validateRequest(request);

        Route route = routeService.create(
                request.getDate(),
                request.getMaxWeight(),
                request.getStatus()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(route);
    }

    @PatchMapping("/{id}")
    public Route update(@PathVariable("id") Long id,
                        @RequestBody Route request) {
        validateRequest(request);

        return routeService.update(
                id,
                request.getDate(),
                request.getMaxWeight(),
                request.getStatus()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        routeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateRequest(Route request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
    }
}