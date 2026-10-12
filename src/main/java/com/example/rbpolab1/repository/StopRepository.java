package com.example.rbpolab1.repository;

import com.example.rbpolab1.model.Stop;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class StopRepository {

    private final Map<Long, Stop> stops = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized List<Stop> findAll() {
        return new ArrayList<>(stops.values());
    }

    public synchronized Optional<Stop> findById(Long id) {
        return Optional.ofNullable(stops.get(id));
    }

    public synchronized boolean existsById(Long id) {
        return stops.containsKey(id);
    }

    public synchronized boolean existsByRouteId(Long routeId) {
        return stops.values().stream()
                .anyMatch(stop -> routeId.equals(stop.getRouteId()));
    }

    public synchronized boolean isSequenceNumberTaken(
            Long routeId, Integer sequenceNumber, Long excludedId) {
        return stops.values().stream()
                .anyMatch(stop ->
                        routeId.equals(stop.getRouteId())
                                && sequenceNumber.equals(stop.getSequenceNumber())
                                && !stop.getId().equals(excludedId));
    }

    public synchronized Stop save(Stop stop) {
        if (stop.getId() == null) {
            stop.setId(nextId++);
        }

        stops.put(stop.getId(), stop);
        return stop;
    }

    public synchronized void deleteById(Long id) {
        stops.remove(id);
    }
}