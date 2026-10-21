package com.example.rbpolab1.service;

import com.example.rbpolab1.model.Route;
import com.example.rbpolab1.model.Shipment;
import com.example.rbpolab1.model.ShipmentAssignment;
import com.example.rbpolab1.model.Stop;
import com.example.rbpolab1.repository.RouteRepository;
import com.example.rbpolab1.repository.ShipmentAssignmentRepository;
import com.example.rbpolab1.repository.StopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class DeliveryService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final ShipmentAssignmentRepository assignmentRepository;

    public DeliveryService(
            RouteRepository routeRepository,
            StopRepository stopRepository,
            ShipmentAssignmentRepository assignmentRepository) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public Route startRoute(Long routeId) {
        Route route = findRoute(routeId);
        requireRouteStatus(route, "PLANNED");

        List<ShipmentAssignment> assignments = deliveriesForRoute(routeId);

        if (assignments.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot start a route without shipments"
            );
        }

        BigDecimal totalWeight = BigDecimal.ZERO;

        for (ShipmentAssignment assignment : assignments) {
            Shipment shipment = assignment.getShipment();

            if (!"ASSIGNED".equals(assignment.getStatus())
                    || !"CREATED".equals(shipment.getStatus())) {
                throw new IllegalStateException(
                        "All deliveries must be ASSIGNED and shipments must be CREATED"
                );
            }

            if (!shipment.getAddress().equals(assignment.getStop().getAddress())) {
                throw new IllegalStateException(
                        "Shipment and stop addresses must match"
                );
            }

            totalWeight = totalWeight.add(shipment.getWeight());
        }

        if (totalWeight.compareTo(route.getMaxWeight()) > 0) {
            throw new IllegalStateException("Route capacity exceeded");
        }

        route.setStatus("IN_PROGRESS");

        for (ShipmentAssignment assignment : assignments) {
            assignment.setStatus("IN_PROGRESS");
            assignment.getShipment().setStatus("IN_TRANSIT");
        }

        return route;
    }

    public ShipmentAssignment completeAssignment(Long assignmentId) {
        ShipmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new NoSuchElementException("Assignment not found"));

        Route route = assignment.getStop().getRoute();
        Shipment shipment = assignment.getShipment();

        requireRouteStatus(route, "IN_PROGRESS");

        if (!"IN_PROGRESS".equals(assignment.getStatus())
                || !"IN_TRANSIT".equals(shipment.getStatus())) {
            throw new IllegalStateException(
                    "Delivery must be IN_PROGRESS and shipment must be IN_TRANSIT"
            );
        }

        Instant completedAt = Instant.now();

        assignment.setStatus("COMPLETED");
        assignment.setCompletedAt(completedAt);
        shipment.setStatus("DELIVERED");
        shipment.setDeliveredAt(completedAt);

        return assignment;
    }

    public Route completeRoute(Long routeId) {
        Route route = findRoute(routeId);
        requireRouteStatus(route, "IN_PROGRESS");

        List<ShipmentAssignment> assignments = deliveriesForRoute(routeId);

        if (assignments.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot complete a route without deliveries"
            );
        }

        for (ShipmentAssignment assignment : assignments) {
            if (!"COMPLETED".equals(assignment.getStatus())
                    || !"DELIVERED".equals(assignment.getShipment().getStatus())) {
                throw new IllegalStateException(
                        "All shipments must be delivered before completing the route"
                );
            }
        }

        route.setStatus("COMPLETED");
        return route;
    }

    public Route cancelRoute(Long routeId) {
        Route route = findRoute(routeId);
        requireRouteStatus(route, "PLANNED");

        List<ShipmentAssignment> assignments = deliveriesForRoute(routeId);

        for (ShipmentAssignment assignment : assignments) {
            if (!"ASSIGNED".equals(assignment.getStatus())
                    || !"CREATED".equals(assignment.getShipment().getStatus())) {
                throw new IllegalStateException(
                        "Only deliveries that have not started can be cancelled"
                );
            }
        }

        route.setStatus("CANCELLED");

        for (ShipmentAssignment assignment : assignments) {
            assignment.setStatus("CANCELLED");
        }

        return route;
    }

    public Route repeatRoute(Long routeId, LocalDate newDate) {
        Route sourceRoute = findRoute(routeId);
        requireRouteStatus(sourceRoute, "COMPLETED");

        if (newDate == null) {
            throw new IllegalArgumentException("New date is required");
        }

        if (!newDate.isAfter(LocalDate.now())
                || !newDate.isAfter(sourceRoute.getDate())) {
            throw new IllegalArgumentException(
                    "New date must be in the future and after the original route date"
            );
        }

        List<Stop> sourceStops = stopRepository.findByRouteId(routeId);

        if (sourceStops.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot repeat a route without stops"
            );
        }

        Route newRoute = routeRepository.save(new Route(
                null, newDate, sourceRoute.getMaxWeight(), "PLANNED"
        ));

        for (Stop sourceStop : sourceStops) {
            Stop newStop = new Stop();
            newStop.setRoute(newRoute);
            newStop.setAddress(sourceStop.getAddress());
            newStop.setSequenceNumber(sourceStop.getSequenceNumber());
            stopRepository.save(newStop);
        }

        return newRoute;
    }

    private Route findRoute(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() ->
                        new NoSuchElementException("Route not found"));
    }

    private void requireRouteStatus(Route route, String expectedStatus) {
        if (!expectedStatus.equals(route.getStatus())) {
            throw new IllegalStateException(
                    "Route must be " + expectedStatus
            );
        }
    }

    private List<ShipmentAssignment> deliveriesForRoute(Long routeId) {
        return assignmentRepository.findByRouteId(routeId).stream()
                .filter(assignment -> !"CANCELLED".equals(assignment.getStatus()))
                .toList();
    }
}