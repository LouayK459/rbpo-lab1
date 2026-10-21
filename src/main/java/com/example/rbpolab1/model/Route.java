package com.example.rbpolab1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "routes", schema = "delivery")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_date", nullable = false)
    private LocalDate date;

    @Column(name = "max_weight", nullable = false, precision = 12, scale = 3)
    private BigDecimal maxWeight;

    @Column(nullable = false, length = 20)
    private String status;

    public Route() {
    }

    public Route(Long id, LocalDate date,
                 BigDecimal maxWeight, String status) {
        this.id = id;
        this.date = date;
        this.maxWeight = maxWeight;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(BigDecimal maxWeight) {
        this.maxWeight = maxWeight;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}