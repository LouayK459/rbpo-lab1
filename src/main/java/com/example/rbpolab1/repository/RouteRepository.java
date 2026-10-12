package com.example.rbpolab1.repository;

import com.example.rbpolab1.model.Route;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RouteRepository {

    private final Map<Long, Route> routes = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized List<Route> findAll() {
        return new ArrayList<>(routes.values());
    }

    public synchronized Optional<Route> findById(Long id) {
        return Optional.ofNullable(routes.get(id));
    }

    public synchronized boolean existsById(Long id) {
        return routes.containsKey(id);
    }

    public synchronized Route save(Route route) {
        if (route.getId() == null) {
            route.setId(nextId++);
        }

        routes.put(route.getId(), route);
        return route;
    }

    public synchronized void deleteById(Long id) {
        routes.remove(id);
    }
}