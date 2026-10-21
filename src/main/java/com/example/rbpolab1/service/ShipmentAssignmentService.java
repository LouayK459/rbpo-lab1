package com.example.rbpolab1.service;

import com.example.rbpolab1.model.Route;
import com.example.rbpolab1.model.Shipment;
import com.example.rbpolab1.model.ShipmentAssignment;
import com.example.rbpolab1.model.Stop;
import com.example.rbpolab1.repository.ShipmentAssignmentRepository;
import com.example.rbpolab1.repository.ShipmentRepository;
import com.example.rbpolab1.repository.StopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class ShipmentAssignmentService {

    private final ShipmentAssignmentRepository assignmentRepository;
    private final ShipmentRepository shipmentRepository;
    private final StopRepository stopRepository;

    public ShipmentAssignmentService(
            ShipmentAssignmentRepository assignmentRepository,
            ShipmentRepository shipmentRepository,
            StopRepository stopRepository) {
        this.assignmentRepository = assignmentRepository;
        this.shipmentRepository = shipmentRepository;
        this.stopRepository = stopRepository;
    }

    public List<ShipmentAssignment> findAll() {
        return assignmentRepository.findAll();
    }

    public ShipmentAssignment findById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Assignment not found"));
    }

    public ShipmentAssignment create(
            Long shipmentId, Long stopId, String status) {
        Shipment shipment = findShipment(shipmentId);
        Stop stop = findStop(stopId);

        String validStatus = validateStatus(
                status == null ? "ASSIGNED" : status
        );

        if (!"ASSIGNED".equals(validStatus)) {
            throw new IllegalStateException(
                    "A new assignment must have status ASSIGNED"
            );
        }

        validateAssignment(shipment, stop, null);

        ShipmentAssignment assignment = new ShipmentAssignment();
        assignment.setShipment(shipment);
        assignment.setStop(stop);
        assignment.setStatus(validStatus);
        assignment.setCreatedAt(Instant.now());

        return assignmentRepository.save(assignment);
    }

    public ShipmentAssignment update(
            Long id, Long shipmentId, Long stopId, String status) {
        ShipmentAssignment existingAssignment = findById(id);

        Long updatedShipmentId = shipmentId == null
                ? existingAssignment.getShipmentId() : shipmentId;

        Long updatedStopId = stopId == null
                ? existingAssignment.getStopId() : stopId;

        String updatedStatus = validateStatus(
                status == null ? existingAssignment.getStatus() : status
        );

        if (!updatedStatus.equals(existingAssignment.getStatus())) {
            throw new IllegalStateException(
                    "Use delivery operations to change assignment status"
            );
        }

        Shipment shipment = findShipment(updatedShipmentId);
        Stop stop = findStop(updatedStopId);

        boolean changed = !Objects.equals(
                existingAssignment.getShipmentId(), updatedShipmentId
        ) || !Objects.equals(
                existingAssignment.getStopId(), updatedStopId
        );

        if (changed) {
            requireEditable(existingAssignment);
            validateAssignment(shipment, stop, id);
            existingAssignment.setShipment(shipment);
            existingAssignment.setStop(stop);
        }

        return assignmentRepository.save(existingAssignment);
    }

    public void delete(Long id) {
        ShipmentAssignment assignment = findById(id);
        requireEditable(assignment);
        assignmentRepository.delete(assignment);
    }

    private Shipment findShipment(Long shipmentId) {
        if (shipmentId == null || shipmentId <= 0) {
            throw new IllegalArgumentException(
                    "Valid shipmentId is required"
            );
        }

        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new NoSuchElementException("Shipment not found"));
    }

    private Stop findStop(Long stopId) {
        if (stopId == null || stopId <= 0) {
            throw new IllegalArgumentException(
                    "Valid stopId is required"
            );
        }

        return stopRepository.findById(stopId)
                .orElseThrow(() ->
                        new NoSuchElementException("Stop not found"));
    }

    private void requireEditable(ShipmentAssignment assignment) {
        if (!"ASSIGNED".equals(assignment.getStatus())
                || !"PLANNED".equals(assignment.getStop().getRoute().getStatus())
                || !"CREATED".equals(assignment.getShipment().getStatus())) {
            throw new IllegalStateException(
                    "Only assignments on a planned route can be changed"
            );
        }
    }

    private void validateAssignment(
            Shipment shipment, Stop stop, Long excludedId) {
        Route route = stop.getRoute();

        if (!"PLANNED".equals(route.getStatus())) {
            throw new IllegalStateException("Route must be PLANNED");
        }

        if (!"CREATED".equals(shipment.getStatus())) {
            throw new IllegalStateException("Shipment must be CREATED");
        }

        if (!shipment.getAddress().equals(stop.getAddress())) {
            throw new IllegalStateException(
                    "Shipment and stop addresses must match"
            );
        }

        if (assignmentRepository.isShipmentAssigned(
                shipment.getId(), excludedId)) {
            throw new IllegalStateException(
                    "Shipment is already assigned"
            );
        }

        BigDecimal totalWeight = assignmentRepository.totalWeightForRoute(
                route.getId(), excludedId
        ).add(shipment.getWeight());

        if (totalWeight.compareTo(route.getMaxWeight()) > 0) {
            throw new IllegalStateException("Route capacity exceeded");
        }
    }

    private String validateStatus(String status) {
        Set<String> allowedStatuses = Set.of(
                "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
        );

        if (!allowedStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    "Status must be ASSIGNED, IN_PROGRESS, COMPLETED or CANCELLED"
            );
        }

        return status;
    }
}