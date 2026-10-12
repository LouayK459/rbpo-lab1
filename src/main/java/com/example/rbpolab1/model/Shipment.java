package com.example.rbpolab1.model;

import java.math.BigDecimal;
import java.time.Instant;

public class Shipment {

    private Long id;
    private Long senderId;
    private String address;
    private BigDecimal weight;
    private String status;
    private Instant createdAt;

    public Shipment() {
    }

    public Shipment(Long id, Long senderId, String address,
                    BigDecimal weight, String status, Instant createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.address = address;
        this.weight = weight;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
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