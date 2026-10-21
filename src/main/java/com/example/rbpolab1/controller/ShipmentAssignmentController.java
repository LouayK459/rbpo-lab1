package com.example.rbpolab1.controller;

import com.example.rbpolab1.model.ShipmentAssignment;
import com.example.rbpolab1.service.DeliveryService;
import com.example.rbpolab1.service.ShipmentAssignmentService;
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
@RequestMapping("/api/shipment-assignments")
public class ShipmentAssignmentController {

    private final ShipmentAssignmentService assignmentService;
    private final DeliveryService deliveryService;

    public ShipmentAssignmentController(
            ShipmentAssignmentService assignmentService,
            DeliveryService deliveryService) {
        this.assignmentService = assignmentService;
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public List<ShipmentAssignment> findAll() {
        return assignmentService.findAll();
    }

    @GetMapping("/{id}")
    public ShipmentAssignment findById(@PathVariable("id") Long id) {
        return assignmentService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ShipmentAssignment> create(
            @RequestBody ShipmentAssignment request) {
        validateRequest(request);

        ShipmentAssignment assignment = assignmentService.create(
                request.getShipmentId(),
                request.getStopId(),
                request.getStatus()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    @PatchMapping("/{id}")
    public ShipmentAssignment update(
            @PathVariable("id") Long id,
            @RequestBody ShipmentAssignment request) {
        validateRequest(request);

        return assignmentService.update(
                id,
                request.getShipmentId(),
                request.getStopId(),
                request.getStatus()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    public ShipmentAssignment complete(@PathVariable("id") Long id) {
        return deliveryService.completeAssignment(id);
    }

    private void validateRequest(ShipmentAssignment request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        if (request.getCreatedAt() != null) {
            throw new IllegalArgumentException(
                    "createdAt is set by the server"
            );
        }

        if (request.getCompletedAt() != null) {
            throw new IllegalArgumentException(
                    "completedAt is set by the server"
            );
        }
    }
}