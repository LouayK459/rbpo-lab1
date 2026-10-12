package com.example.rbpolab1.service;

import com.example.rbpolab1.model.Stop;
import com.example.rbpolab1.repository.RouteRepository;
import com.example.rbpolab1.repository.StopRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class StopService {

    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;

    public StopService(StopRepository stopRepository,
                       RouteRepository routeRepository) {
        this.stopRepository = stopRepository;
        this.routeRepository = routeRepository;
    }

    public List<Stop> findAll() {
        return stopRepository.findAll();
    }

    public Stop findById(Long id) {
        return stopRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Stop not found"));
    }

    public synchronized Stop create(Long routeId, String address,
                                    Integer sequenceNumber) {
        validateRoute(routeId);
        String validAddress = validateAddress(address);
        validateSequenceNumber(routeId, sequenceNumber, null);

        Stop stop = new Stop(
                null,
                routeId,
                validAddress,
                sequenceNumber
        );

        return stopRepository.save(stop);
    }

    public synchronized Stop update(Long id, Long routeId,
                                    String address,
                                    Integer sequenceNumber) {
        Stop existingStop = findById(id);

        Long updatedRouteId = routeId == null
                ? existingStop.getRouteId() : routeId;

        String updatedAddress = address == null
                ? existingStop.getAddress() : address;

        Integer updatedSequenceNumber = sequenceNumber == null
                ? existingStop.getSequenceNumber() : sequenceNumber;

        validateRoute(updatedRouteId);
        updatedAddress = validateAddress(updatedAddress);
        validateSequenceNumber(
                updatedRouteId,
                updatedSequenceNumber,
                id
        );

        Stop updatedStop = new Stop(
                id,
                updatedRouteId,
                updatedAddress,
                updatedSequenceNumber
        );

        return stopRepository.save(updatedStop);
    }

    public synchronized void delete(Long id) {
        findById(id);
        stopRepository.deleteById(id);
    }

    private void validateRoute(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw new IllegalArgumentException(
                    "Valid routeId is required"
            );
        }

        if (!routeRepository.existsById(routeId)) {
            throw new NoSuchElementException("Route not found");
        }
    }

    private String validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }

        return address.trim();
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
}