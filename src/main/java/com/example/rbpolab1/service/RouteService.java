package com.example.rbpolab1.service;

import com.example.rbpolab1.model.Route;
import com.example.rbpolab1.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class RouteService {

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public List<Route> findAll() {
        return routeRepository.findAll();
    }

    public Route findById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Route not found"));
    }

    public synchronized Route create(LocalDate date,
                                     BigDecimal maxWeight,
                                     String status) {
        validateDate(date);
        validateMaxWeight(maxWeight);

        String validStatus = validateStatus(
                status == null ? "PLANNED" : status
        );

        Route route = new Route(null, date, maxWeight, validStatus);
        return routeRepository.save(route);
    }

    public synchronized Route update(Long id, LocalDate date,
                                     BigDecimal maxWeight,
                                     String status) {
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

        Route updatedRoute = new Route(
                id,
                updatedDate,
                updatedMaxWeight,
                updatedStatus
        );

        return routeRepository.save(updatedRoute);
    }

    public synchronized void delete(Long id) {
        findById(id);
        routeRepository.deleteById(id);
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
    }

    private String validateStatus(String status) {
        Set<String> allowedStatuses = Set.of(
                "PLANNED", "IN_PROGRESS", "COMPLETED"
        );

        if (!allowedStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    "Status must be PLANNED, IN_PROGRESS or COMPLETED"
            );
        }

        return status;
    }
}