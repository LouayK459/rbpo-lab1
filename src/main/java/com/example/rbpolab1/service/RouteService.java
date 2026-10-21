package com.example.rbpolab1.service;

import com.example.rbpolab1.model.Route;
import com.example.rbpolab1.repository.RouteRepository;
import com.example.rbpolab1.repository.ShipmentAssignmentRepository;
import com.example.rbpolab1.repository.StopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class RouteService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final ShipmentAssignmentRepository assignmentRepository;

    public RouteService(
            RouteRepository routeRepository,
            StopRepository stopRepository,
            ShipmentAssignmentRepository assignmentRepository) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<Route> findAll() {
        return routeRepository.findAll();
    }

    public Route findById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Route not found"));
    }

    public Route create(LocalDate date, BigDecimal maxWeight,
                        String status) {
        validateDate(date);
        validateMaxWeight(maxWeight);

        String validStatus = validateStatus(
                status == null ? "PLANNED" : status
        );

        if (!"PLANNED".equals(validStatus)) {
            throw new IllegalStateException(
                    "A new route must have status PLANNED"
            );
        }

        Route route = new Route(null, date, maxWeight, validStatus);
        return routeRepository.save(route);
    }

    public Route update(Long id, LocalDate date,
                        BigDecimal maxWeight, String status) {
        Route existingRoute = findById(id);

        LocalDate updatedDate = date == null
                ? existingRoute.getDate() : date;

        BigDecimal updatedMaxWeight = maxWeight == null
                ? existingRoute.getMaxWeight() : maxWeight;

        String updatedStatus = status == null
                ? existingRoute.getStatus() : status;

        validateDate(updatedDate);
        validateMaxWeight(updatedMaxWeight);
        updatedStatus = validateStatus(updatedStatus);

        if (!updatedStatus.equals(existingRoute.getStatus())) {
            throw new IllegalStateException(
                    "Use delivery operations to change route status"
            );
        }

        boolean changed = !updatedDate.equals(existingRoute.getDate())
                || updatedMaxWeight.compareTo(existingRoute.getMaxWeight()) != 0;

        if (changed) {
            requirePlanned(existingRoute);

            BigDecimal assignedWeight = assignmentRepository.totalWeightForRoute(
                    id, null
            );

            if (updatedMaxWeight.compareTo(assignedWeight) < 0) {
                throw new IllegalStateException(
                        "Max weight cannot be less than the assigned shipment weight"
                );
            }
        }

        existingRoute.setDate(updatedDate);
        existingRoute.setMaxWeight(updatedMaxWeight);

        return routeRepository.save(existingRoute);
    }

    public void delete(Long id) {
        Route route = findById(id);
        requirePlanned(route);

        if (stopRepository.existsByRouteId(id)) {
            throw new IllegalStateException(
                    "Cannot delete a route with stops"
            );
        }

        routeRepository.delete(route);
    }

    private void requirePlanned(Route route) {
        if (!"PLANNED".equals(route.getStatus())) {
            throw new IllegalStateException(
                    "Only a route with status PLANNED can be changed"
            );
        }
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
    }

    private void validateMaxWeight(BigDecimal maxWeight) {
        if (maxWeight == null
                || maxWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Max weight must be positive"
            );
        }

        if (maxWeight.compareTo(new BigDecimal("1000000000")) >= 0
                || maxWeight.stripTrailingZeros().scale() > 3) {
            throw new IllegalArgumentException(
                    "Max weight must be less than 1000000000 and have at most 3 decimal places"
            );
        }
    }

    private String validateStatus(String status) {
        Set<String> allowedStatuses = Set.of(
                "PLANNED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
        );

        if (!allowedStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    "Status must be PLANNED, IN_PROGRESS, COMPLETED or CANCELLED"
            );
        }

        return status;
    }
}