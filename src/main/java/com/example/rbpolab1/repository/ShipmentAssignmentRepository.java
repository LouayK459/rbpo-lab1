package com.example.rbpolab1.repository;

import com.example.rbpolab1.model.ShipmentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ShipmentAssignmentRepository
        extends JpaRepository<ShipmentAssignment, Long> {

    @Query("select count(a) > 0 from ShipmentAssignment a where a.shipment.id = :shipmentId")
    boolean existsByShipmentId(@Param("shipmentId") Long shipmentId);

    @Query("select count(a) > 0 from ShipmentAssignment a where a.stop.id = :stopId")
    boolean existsByStopId(@Param("stopId") Long stopId);

    @Query("""
            select count(a) > 0
            from ShipmentAssignment a
            where a.shipment.id = :shipmentId
              and a.status in ('ASSIGNED', 'IN_PROGRESS')
              and (:excludedId is null or a.id <> :excludedId)
            """)
    boolean isShipmentAssigned(
            @Param("shipmentId") Long shipmentId,
            @Param("excludedId") Long excludedId);

    @Query("""
            select coalesce(sum(a.shipment.weight), 0)
            from ShipmentAssignment a
            where a.stop.route.id = :routeId
              and a.status in ('ASSIGNED', 'IN_PROGRESS')
              and (:excludedId is null or a.id <> :excludedId)
            """)
    BigDecimal totalWeightForRoute(
            @Param("routeId") Long routeId,
            @Param("excludedId") Long excludedId);

    @Query("""
            select a from ShipmentAssignment a
            join fetch a.shipment
            join fetch a.stop
            where a.stop.route.id = :routeId
            order by a.stop.sequenceNumber, a.id
            """)
    List<ShipmentAssignment> findByRouteId(@Param("routeId") Long routeId);
}