package com.example.rbpolab1.repository;

import com.example.rbpolab1.model.Shipment;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ShipmentRepository {

    private final Map<Long, Shipment> shipments = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized List<Shipment> findAll() {
        return new ArrayList<>(shipments.values());
    }

    public synchronized Optional<Shipment> findById(Long id) {
        return Optional.ofNullable(shipments.get(id));
    }

    public synchronized boolean existsById(Long id) {
        return shipments.containsKey(id);
    }

    public synchronized boolean existsBySenderId(Long senderId) {
        return shipments.values().stream()
                .anyMatch(shipment -> senderId.equals(shipment.getSenderId()));
    }

    public synchronized Shipment save(Shipment shipment) {
        if (shipment.getId() == null) {
            shipment.setId(nextId++);
        }

        shipments.put(shipment.getId(), shipment);
        return shipment;
    }

    public synchronized void deleteById(Long id) {
        shipments.remove(id);
    }
}