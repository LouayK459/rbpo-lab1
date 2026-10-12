package com.example.rbpolab1.repository;

import com.example.rbpolab1.model.ShipmentAssignment;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ShipmentAssignmentRepository {

    private final Map<Long, ShipmentAssignment> assignments =
            new LinkedHashMap<>();

    private long nextId = 1;

    public synchronized List<ShipmentAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    public synchronized Optional<ShipmentAssignment> findById(Long id) {
        return Optional.ofNullable(assignments.get(id));
    }

    public synchronized boolean existsByShipmentId(Long shipmentId) {
        return assignments.values().stream()
                .anyMatch(assignment ->
                        shipmentId.equals(assignment.getShipmentId()));
    }

    public synchronized boolean existsByStopId(Long stopId) {
        return assignments.values().stream()
                .anyMatch(assignment ->
                        stopId.equals(assignment.getStopId()));
    }

    public synchronized boolean isShipmentAssigned(
            Long shipmentId, Long excludedId) {
        return assignments.values().stream()
                .anyMatch(assignment ->
                        shipmentId.equals(assignment.getShipmentId())
                                && !assignment.getId().equals(excludedId));
    }

    public synchronized ShipmentAssignment save(
            ShipmentAssignment assignment) {
        if (assignment.getId() == null) {
            assignment.setId(nextId++);
        }

        assignments.put(assignment.getId(), assignment);
        return assignment;
    }

    public synchronized void deleteById(Long id) {
        assignments.remove(id);
    }
}