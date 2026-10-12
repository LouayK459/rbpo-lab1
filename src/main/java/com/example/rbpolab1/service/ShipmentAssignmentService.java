package com.example.rbpolab1.service;

import com.example.rbpolab1.model.ShipmentAssignment;
import com.example.rbpolab1.repository.ShipmentAssignmentRepository;
import com.example.rbpolab1.repository.ShipmentRepository;
import com.example.rbpolab1.repository.StopRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
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

    public synchronized ShipmentAssignment create(
            Long shipmentId, Long stopId, String status) {
        validateShipment(shipmentId);
        validateStop(stopId);
        validateUniqueShipment(shipmentId, null);

        String validStatus = validateStatus(
                status == null ? "ASSIGNED" : status
        );

        ShipmentAssignment assignment = new ShipmentAssignment(
                null,
                shipmentId,
                stopId,
                validStatus,
                Instant.now()
        );

        return assignmentRepository.save(assignment);
    }

    public synchronized ShipmentAssignment update(
            Long id, Long shipmentId, Long stopId, String status) {
        ShipmentAssignment existingAssignment = findById(id);

        Long updatedShipmentId = shipmentId == null
                ? existingAssignment.getShipmentId() : shipmentId;

        Long updatedStopId = stopId == null
                ? existingAssignment.getStopId() : stopId;

        String updatedStatus = status == null
                ? existingAssignment.getStatus() : status;

        validateShipment(updatedShipmentId);
        validateStop(updatedStopId);
        validateUniqueShipment(updatedShipmentId, id);
        updatedStatus = validateStatus(updatedStatus);

        ShipmentAssignment updatedAssignment = new ShipmentAssignment(
                id,
                updatedShipmentId,
                updatedStopId,
                updatedStatus,
                existingAssignment.getCreatedAt()
        );

        return assignmentRepository.save(updatedAssignment);
    }

    public synchronized void delete(Long id) {
        findById(id);
        assignmentRepository.deleteById(id);
    }

    private void validateShipment(Long shipmentId) {
        if (shipmentId == null || shipmentId <= 0) {
            throw new IllegalArgumentException(
                    "Valid shipmentId is required"
            );
        }

        if (!shipmentRepository.existsById(shipmentId)) {
            throw new NoSuchElementException("Shipment not found");
        }
    }

    private void validateStop(Long stopId) {
        if (stopId == null || stopId <= 0) {
            throw new IllegalArgumentException(
                    "Valid stopId is required"
            );
        }

        if (!stopRepository.existsById(stopId)) {
            throw new NoSuchElementException("Stop not found");
        }
    }

    private void validateUniqueShipment(Long shipmentId, Long excludedId) {
        if (assignmentRepository.isShipmentAssigned(
                shipmentId, excludedId)) {
            throw new IllegalStateException(
                    "Shipment is already assigned"
            );
        }
    }

    private String validateStatus(String status) {
        Set<String> allowedStatuses = Set.of(
                "ASSIGNED", "IN_PROGRESS", "COMPLETED"
        );

        if (!allowedStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    "Status must be ASSIGNED, IN_PROGRESS or COMPLETED"
            );
        }

        return status;
    }
}