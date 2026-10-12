package com.example.rbpolab1.model;

public class Stop {

    private Long id;
    private Long routeId;
    private String address;
    private Integer sequenceNumber;

    public Stop() {
    }

    public Stop(Long id, Long routeId, String address,
                Integer sequenceNumber) {
        this.id = id;
        this.routeId = routeId;
        this.address = address;
        this.sequenceNumber = sequenceNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}