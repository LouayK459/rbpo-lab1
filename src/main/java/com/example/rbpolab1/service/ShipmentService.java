package com.example.rbpolab1.service;

import com.example.rbpolab1.model.Shipment;
import com.example.rbpolab1.model.User;
import com.example.rbpolab1.repository.ShipmentAssignmentRepository;
import com.example.rbpolab1.repository.ShipmentRepository;
import com.example.rbpolab1.repository.UserRepository;
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
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final ShipmentAssignmentRepository assignmentRepository;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            UserRepository userRepository,
            ShipmentAssignmentRepository assignmentRepository) {
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<Shipment> findAll() {
        return shipmentRepository.findAll();
    }

    public Shipment findById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Shipment not found"));
    }

    public Shipment create(Long senderId, String address,
                           BigDecimal weight, String status) {
        User sender = validateSender(senderId);
        String validAddress = validateAddress(address);
        validateWeight(weight);

        String validStatus = validateStatus(
                status == null ? "CREATED" : status
        );

        if (!"CREATED".equals(validStatus)) {
            throw new IllegalStateException(
                    "A new shipment must have status CREATED"
            );
        }

        Shipment shipment = new Shipment();
        shipment.setSender(sender);
        shipment.setAddress(validAddress);
        shipment.setWeight(weight);
        shipment.setStatus(validStatus);
        shipment.setCreatedAt(Instant.now());

        return shipmentRepository.save(shipment);
    }

    public Shipment update(Long id, Long senderId,
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

        User sender = validateSender(updatedSenderId);
        updatedAddress = validateAddress(updatedAddress);
        validateWeight(updatedWeight);
        updatedStatus = validateStatus(updatedStatus);

        if (!updatedStatus.equals(existingShipment.getStatus())) {
            throw new IllegalStateException(
                    "Use delivery operations to change shipment status"
            );
        }

        boolean changed = !Objects.equals(
                updatedSenderId, existingShipment.getSenderId()
        ) || !Objects.equals(
                updatedAddress, existingShipment.getAddress()
        ) || updatedWeight.compareTo(existingShipment.getWeight()) != 0;

        if (changed && (!"CREATED".equals(existingShipment.getStatus())
                || assignmentRepository.isShipmentAssigned(id, null))) {
            throw new IllegalStateException(
                    "Cannot edit a shipment with an active delivery or after dispatch"
            );
        }

        existingShipment.setSender(sender);
        existingShipment.setAddress(updatedAddress);
        existingShipment.setWeight(updatedWeight);

        return shipmentRepository.save(existingShipment);
    }

    public void delete(Long id) {
        Shipment existingShipment = findById(id);

        if (!"CREATED".equals(existingShipment.getStatus())) {
            throw new IllegalStateException(
                    "Only a shipment with status CREATED can be deleted"
            );
        }

        if (assignmentRepository.existsByShipmentId(id)) {
            throw new IllegalStateException(
                    "Cannot delete a shipment with assignments"
            );
        }

        shipmentRepository.delete(existingShipment);
    }

    private User validateSender(Long senderId) {
        if (senderId == null || senderId <= 0) {
            throw new IllegalArgumentException(
                    "Valid senderId is required"
            );
        }

        return userRepository.findById(senderId)
                .orElseThrow(() ->
                        new NoSuchElementException("Sender not found"));
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

    private void validateWeight(BigDecimal weight) {
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }

        if (weight.compareTo(new BigDecimal("1000000000")) >= 0
                || weight.stripTrailingZeros().scale() > 3) {
            throw new IllegalArgumentException(
                    "Weight must be less than 1000000000 and have at most 3 decimal places"
            );
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