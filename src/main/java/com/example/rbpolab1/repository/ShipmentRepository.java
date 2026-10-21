package com.example.rbpolab1.repository;

import com.example.rbpolab1.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @Query("select count(s) > 0 from Shipment s where s.sender.id = :senderId")
    boolean existsBySenderId(@Param("senderId") Long senderId);
}