package com.example.rbpolab1.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Route {

    private Long id;
    private LocalDate date;
    private BigDecimal maxWeight;
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