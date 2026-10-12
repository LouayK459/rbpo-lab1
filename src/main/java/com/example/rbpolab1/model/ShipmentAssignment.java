package com.example.rbpolab1.model;

import java.time.Instant;

public class ShipmentAssignment {

    private Long id;
    private Long shipmentId;
    private Long stopId;
    private String status;
    private Instant createdAt;

    public ShipmentAssignment() {
    }

    public ShipmentAssignment(Long id, Long shipmentId, Long stopId,
                              String status, Instant createdAt) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.stopId = stopId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Long getStopId() {
        return stopId;
    }

    public void setStopId(Long stopId) {
        this.stopId = stopId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}