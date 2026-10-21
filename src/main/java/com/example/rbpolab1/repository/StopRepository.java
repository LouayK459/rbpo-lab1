package com.example.rbpolab1.repository;

import com.example.rbpolab1.model.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StopRepository extends JpaRepository<Stop, Long> {

    @Query("select count(s) > 0 from Stop s where s.route.id = :routeId")
    boolean existsByRouteId(@Param("routeId") Long routeId);

    @Query("""
            select count(s) > 0
            from Stop s
            where s.route.id = :routeId
              and s.sequenceNumber = :sequenceNumber
              and (:excludedId is null or s.id <> :excludedId)
            """)
    boolean isSequenceNumberTaken(
            @Param("routeId") Long routeId,
            @Param("sequenceNumber") Integer sequenceNumber,
            @Param("excludedId") Long excludedId);

    @Query("select s from Stop s where s.route.id = :routeId order by s.sequenceNumber")
    List<Stop> findByRouteId(@Param("routeId") Long routeId);
}