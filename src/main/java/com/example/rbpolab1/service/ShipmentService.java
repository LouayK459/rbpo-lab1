package com.example.rbpolab1.service;

import com.example.rbpolab1.model.Shipment;
import com.example.rbpolab1.repository.ShipmentRepository;
import com.example.rbpolab1.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           UserRepository userRepository) {
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
    }

    public List<Shipment> findAll() {
        return shipmentRepository.findAll();
    }

    public Shipment findById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Shipment not found"));
    }

    public synchronized Shipment create(Long senderId, String address,
                                        BigDecimal weight, String status) {
        validateSender(senderId);
        String validAddress = validateAddress(address);
        validateWeight(weight);
        String validStatus = validateStatus(
                status == null ? "CREATED" : status
        );

        Shipment shipment = new Shipment(
                null,
                senderId,
                validAddress,
                weight,
                validStatus,
                Instant.now()
        );

        return shipmentRepository.save(shipment);
    }

    public synchronized Shipment update(Long id, Long senderId,
                                        String address, BigDecimal weight,
                                        String status) {
        Shipment existingShipment = findById(id);

        Long updatedSenderId = senderId == null
                ? existingShipment.getSenderId() : senderId;

        String updatedAddress = address == null
                ? existingShipment.getAddress() : address;

        BigDecimal updatedWeight = weight == null
                ? existingShipment.getWeight() : weight;

        String updatedStatus = status == null
                ? existingShipment.getStatus() : status;

        validateSender(updatedSenderId);
        updatedAddress = validateAddress(updatedAddress);
        validateWeight(updatedWeight);
        updatedStatus = validateStatus(updatedStatus);

        Shipment updatedShipment = new Shipment(
                id,
                updatedSenderId,
                updatedAddress,
                updatedWeight,
                updatedStatus,
                existingShipment.getCreatedAt()
        );

        return shipmentRepository.save(updatedShipment);
    }

    public synchronized void delete(Long id) {
        findById(id);
        shipmentRepository.deleteById(id);
    }

    private void validateSender(Long senderId) {
        if (senderId == null || senderId <= 0) {
            throw new IllegalArgumentException("Valid senderId is required");
        }

        if (!userRepository.existsById(senderId)) {
            throw new NoSuchElementException("Sender not found");
        }
    }

    private String validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }

        return address.trim();
    }

    private void validateWeight(BigDecimal weight) {
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
    }

    private String validateStatus(String status) {
        Set<String> allowedStatuses = Set.of(
                "CREATED", "IN_TRANSIT", "DELIVERED"
        );

        if (!allowedStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    "Status must be CREATED, IN_TRANSIT or DELIVERED"
            );
        }

        return status;
    }
}