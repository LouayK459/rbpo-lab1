package com.example.rbpolab1.service;

import com.example.rbpolab1.model.Stop;
import com.example.rbpolab1.model.Route;
import com.example.rbpolab1.repository.RouteRepository;
import com.example.rbpolab1.repository.ShipmentAssignmentRepository;
import com.example.rbpolab1.repository.StopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class StopService {

    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final ShipmentAssignmentRepository assignmentRepository;

    public StopService(
            StopRepository stopRepository,
            RouteRepository routeRepository,
            ShipmentAssignmentRepository assignmentRepository) {
        this.stopRepository = stopRepository;
        this.routeRepository = routeRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<Stop> findAll() {
        return stopRepository.findAll();
    }

    public Stop findById(Long id) {
        return stopRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Stop not found"));
    }

    public Stop create(Long routeId, String address,
                       Integer sequenceNumber) {
        Route route = validateRoute(routeId);
        requirePlanned(route);
        String validAddress = validateAddress(address);
        validateSequenceNumber(routeId, sequenceNumber, null);

        Stop stop = new Stop();
        stop.setRoute(route);
        stop.setAddress(validAddress);
        stop.setSequenceNumber(sequenceNumber);

        return stopRepository.save(stop);
    }

    public Stop update(Long id, Long routeId,
                       String address, Integer sequenceNumber) {
        Stop existingStop = findById(id);

        Long updatedRouteId = routeId == null
                ? existingStop.getRouteId() : routeId;

        String updatedAddress = address == null
                ? existingStop.getAddress() : address;

        Integer updatedSequenceNumber = sequenceNumber == null
                ? existingStop.getSequenceNumber() : sequenceNumber;

        Route updatedRoute = validateRoute(updatedRouteId);
        updatedAddress = validateAddress(updatedAddress);
        validateSequenceNumber(
                updatedRouteId,
                updatedSequenceNumber,
                id
        );

        boolean routeChanged = !Objects.equals(
                updatedRouteId, existingStop.getRouteId()
        );
        boolean addressChanged = !Objects.equals(
                updatedAddress, existingStop.getAddress()
        );
        boolean sequenceChanged = !Objects.equals(
                updatedSequenceNumber, existingStop.getSequenceNumber()
        );

        if (routeChanged || addressChanged || sequenceChanged) {
            requirePlanned(existingStop.getRoute());
            requirePlanned(updatedRoute);

            if ((routeChanged || addressChanged)
                    && assignmentRepository.existsByStopId(id)) {
                throw new IllegalStateException(
                        "Cannot move a stop or change its address while it has assignments"
                );
            }
        }

        existingStop.setRoute(updatedRoute);
        existingStop.setAddress(updatedAddress);
        existingStop.setSequenceNumber(updatedSequenceNumber);

        return stopRepository.save(existingStop);
    }

    public void delete(Long id) {
        Stop existingStop = findById(id);
        requirePlanned(existingStop.getRoute());

        if (assignmentRepository.existsByStopId(id)) {
            throw new IllegalStateException(
                    "Cannot delete a stop with assignments"
            );
        }

        stopRepository.delete(existingStop);
    }

    private Route validateRoute(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw new IllegalArgumentException(
                    "Valid routeId is required"
            );
        }

        return routeRepository.findById(routeId)
                .orElseThrow(() ->
                        new NoSuchElementException("Route not found"));
    }

    private String validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }

        String validAddress = address.trim();

        if (validAddress.length() > 255) {
            throw new IllegalArgumentException(
                    "Address must not exceed 255 characters"
            );
        }

        return validAddress;
    }

    private void validateSequenceNumber(Long routeId,
                                        Integer sequenceNumber,
                                        Long excludedId) {
        if (sequenceNumber == null || sequenceNumber <= 0) {
            throw new IllegalArgumentException(
                    "Sequence number must be positive"
            );
        }

        if (stopRepository.isSequenceNumberTaken(
                routeId, sequenceNumber, excludedId)) {
            throw new IllegalStateException(
                    "Sequence number is already used in this route"
            );
        }
    }

    private void requirePlanned(Route route) {
        if (!"PLANNED".equals(route.getStatus())) {
            throw new IllegalStateException(
                    "Stops can only be changed while the route is PLANNED"
            );
        }
    }
}