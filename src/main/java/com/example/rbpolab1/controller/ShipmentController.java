package com.example.rbpolab1.controller;

import com.example.rbpolab1.model.Shipment;
import com.example.rbpolab1.service.ShipmentService;
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
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public List<Shipment> findAll() {
        return shipmentService.findAll();
    }

    @GetMapping("/{id}")
    public Shipment findById(@PathVariable("id") Long id) {
        return shipmentService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Shipment> create(@RequestBody Shipment request) {
        validateRequest(request);

        Shipment shipment = shipmentService.create(
                request.getSenderId(),
                request.getAddress(),
                request.getWeight(),
                request.getStatus()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
    }

    @PatchMapping("/{id}")
    public Shipment update(@PathVariable("id") Long id,
                           @RequestBody Shipment request) {
        validateRequest(request);

        return shipmentService.update(
                id,
                request.getSenderId(),
                request.getAddress(),
                request.getWeight(),
                request.getStatus()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        shipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateRequest(Shipment request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        if (request.getCreatedAt() != null) {
            throw new IllegalArgumentException(
                    "createdAt is set by the server"
            );
        }

        if (request.getDeliveredAt() != null) {
            throw new IllegalArgumentException(
                    "deliveredAt is set by the server"
            );
        }
    }
}