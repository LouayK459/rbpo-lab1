package com.example.rbpolab1;

import com.example.rbpolab1.model.Route;
import com.example.rbpolab1.model.Shipment;
import com.example.rbpolab1.model.ShipmentAssignment;
import com.example.rbpolab1.model.Stop;
import com.example.rbpolab1.model.User;
import com.example.rbpolab1.repository.ShipmentAssignmentRepository;
import com.example.rbpolab1.repository.StopRepository;
import com.example.rbpolab1.service.DeliveryService;
import com.example.rbpolab1.service.RouteService;
import com.example.rbpolab1.service.ShipmentAssignmentService;
import com.example.rbpolab1.service.ShipmentService;
import com.example.rbpolab1.service.StopService;
import com.example.rbpolab1.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional(isolation = Isolation.SERIALIZABLE)
class Lab3DatabaseTests {

    private static final String ADDRESS = "Moscow, Aviamotornaya 8";

    @Autowired
    private UserService userService;
    @Autowired
    private RouteService routeService;
    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private StopService stopService;
    @Autowired
    private ShipmentAssignmentService assignmentService;
    @Autowired
    private DeliveryService deliveryService;
    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private ShipmentAssignmentRepository assignmentRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void crudPersistsChangesAndDeletesInDependencyOrder() {
        User user = userService.create("Lab 3 test");
        userService.update(user.getId(), "Updated test user");

        Route route = routeService.create(LocalDate.now(), decimal("20"), null);
        routeService.update(route.getId(), LocalDate.now().plusDays(1), decimal("25"), null);

        Shipment shipment = shipmentService.create(user.getId(), ADDRESS, decimal("2.5"), null);
        shipmentService.update(shipment.getId(), null, null, decimal("3.5"), null);

        Stop stop = stopService.create(route.getId(), ADDRESS, 1);
        stopService.update(stop.getId(), null, null, 2);
        Stop otherStop = stopService.create(route.getId(), ADDRESS, 3);

        ShipmentAssignment assignment = assignmentService.create(shipment.getId(), stop.getId(), null);
        reload();
        Instant createdAt = assignmentService.findById(assignment.getId()).getCreatedAt();
        assignmentService.update(assignment.getId(), null, otherStop.getId(), null);
        reload();

        assertEquals("Updated test user", userService.findById(user.getId()).getName());
        assertEquals(0, routeService.findById(route.getId()).getMaxWeight().compareTo(decimal("25")));
        assertEquals(0, shipmentService.findById(shipment.getId()).getWeight().compareTo(decimal("3.5")));
        assertEquals(2, stopService.findById(stop.getId()).getSequenceNumber());
        assertEquals(otherStop.getId(), assignmentService.findById(assignment.getId()).getStopId());
        assertEquals(createdAt, assignmentService.findById(assignment.getId()).getCreatedAt());

        assignmentService.delete(assignment.getId());
        stopService.delete(otherStop.getId());
        stopService.delete(stop.getId());
        shipmentService.delete(shipment.getId());
        routeService.delete(route.getId());
        userService.delete(user.getId());
        reload();

        assertNull(entityManager.find(ShipmentAssignment.class, assignment.getId()));
        assertNull(entityManager.find(Stop.class, stop.getId()));
        assertNull(entityManager.find(Stop.class, otherStop.getId()));
        assertNull(entityManager.find(Shipment.class, shipment.getId()));
        assertNull(entityManager.find(Route.class, route.getId()));
        assertNull(entityManager.find(User.class, user.getId()));
    }

    @Test
    void deliveryWorkflowUpdatesRelatedRowsAndRecordsHandover() {
        Fixture fixture = fixture("2.5", "20");
        ShipmentAssignment assignment = assignmentService.create(
                fixture.shipment().getId(), fixture.stop().getId(), null);
        reload();
        Instant createdAt = shipmentService.findById(fixture.shipment().getId()).getCreatedAt();

        deliveryService.startRoute(fixture.route().getId());
        reload();
        assertEquals("IN_PROGRESS", routeService.findById(fixture.route().getId()).getStatus());
        assertEquals("IN_PROGRESS", assignmentService.findById(assignment.getId()).getStatus());
        assertEquals("IN_TRANSIT", shipmentService.findById(fixture.shipment().getId()).getStatus());

        deliveryService.completeAssignment(assignment.getId());
        deliveryService.completeRoute(fixture.route().getId());
        reload();

        Shipment delivered = shipmentService.findById(fixture.shipment().getId());
        ShipmentAssignment completed = assignmentService.findById(assignment.getId());
        assertEquals("COMPLETED", routeService.findById(fixture.route().getId()).getStatus());
        assertEquals("COMPLETED", completed.getStatus());
        assertEquals("DELIVERED", delivered.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertEquals(completed.getCompletedAt(), delivered.getDeliveredAt());
        assertFalse(delivered.getDeliveredAt().isBefore(createdAt));
        assertEquals(createdAt, delivered.getCreatedAt());
    }

    @Test
    void repeatingCompletedRouteCopiesStopsWithoutCopyingShipments() {
        Fixture fixture = fixture("2.5", "20");
        ShipmentAssignment assignment = assignmentService.create(
                fixture.shipment().getId(), fixture.stop().getId(), null);
        deliveryService.startRoute(fixture.route().getId());
        deliveryService.completeAssignment(assignment.getId());
        deliveryService.completeRoute(fixture.route().getId());

        LocalDate newDate = LocalDate.now().plusDays(2);
        Route repeated = deliveryService.repeatRoute(fixture.route().getId(), newDate);
        reload();

        List<Stop> stops = stopRepository.findByRouteId(repeated.getId());
        assertNotEquals(fixture.route().getId(), repeated.getId());
        assertEquals("PLANNED", routeService.findById(repeated.getId()).getStatus());
        assertEquals(newDate, routeService.findById(repeated.getId()).getDate());
        assertEquals(1, stops.size());
        assertNotEquals(fixture.stop().getId(), stops.getFirst().getId());
        assertEquals(ADDRESS, stops.getFirst().getAddress());
        assertEquals(1, stops.getFirst().getSequenceNumber());
        assertTrue(assignmentRepository.findByRouteId(repeated.getId()).isEmpty());
    }

    @Test
    void cancellingRouteReleasesShipmentForAnotherRoute() {
        Fixture fixture = fixture("3.5", "20");
        ShipmentAssignment original = assignmentService.create(
                fixture.shipment().getId(), fixture.stop().getId(), null);
        deliveryService.cancelRoute(fixture.route().getId());
        reload();

        assertEquals("CANCELLED", routeService.findById(fixture.route().getId()).getStatus());
        assertEquals("CANCELLED", assignmentService.findById(original.getId()).getStatus());
        assertNull(assignmentService.findById(original.getId()).getCompletedAt());
        assertEquals("CREATED", shipmentService.findById(fixture.shipment().getId()).getStatus());
        assertNull(shipmentService.findById(fixture.shipment().getId()).getDeliveredAt());

        Route otherRoute = routeService.create(LocalDate.now().plusDays(1), decimal("20"), null);
        Stop otherStop = stopService.create(otherRoute.getId(), ADDRESS, 1);
        ShipmentAssignment replacement = assignmentService.create(
                fixture.shipment().getId(), otherStop.getId(), null);
        reload();

        assertNotEquals(original.getId(), replacement.getId());
        assertEquals("ASSIGNED", assignmentService.findById(replacement.getId()).getStatus());
        assertEquals("CANCELLED", assignmentService.findById(original.getId()).getStatus());
    }

    @Test
    void overweightShipmentIsRejectedWithoutCreatingAssignment() {
        Fixture fixture = fixture("21", "20");
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> assignmentService.create(fixture.shipment().getId(), fixture.stop().getId(), null));
        assertEquals("Route capacity exceeded", failure.getMessage());
        reload();
        assertFalse(assignmentRepository.existsByShipmentId(fixture.shipment().getId()));
        assertEquals("CREATED", shipmentService.findById(fixture.shipment().getId()).getStatus());
    }

    @Test
    void assignedShipmentCannotBeDuplicatedOrUsedToBypassCapacity() {
        Fixture fixture = fixture("3.5", "20");
        assignmentService.create(fixture.shipment().getId(), fixture.stop().getId(), null);

        assertThrows(IllegalStateException.class, () -> assignmentService.create(
                fixture.shipment().getId(), fixture.stop().getId(), null));
        assertThrows(IllegalStateException.class, () -> routeService.update(
                fixture.route().getId(), null, decimal("3"), null));
        assertThrows(IllegalStateException.class, () -> shipmentService.update(
                fixture.shipment().getId(), null, null, decimal("25"), null));
        reload();

        assertEquals(1, assignmentRepository.findByRouteId(fixture.route().getId()).size());
        assertEquals(0, routeService.findById(fixture.route().getId()).getMaxWeight().compareTo(decimal("20")));
        assertEquals(0, shipmentService.findById(fixture.shipment().getId()).getWeight().compareTo(decimal("3.5")));
    }

    @Test
    void handoverCannotBeRecordedBeforeRouteStarts() {
        Fixture fixture = fixture("2.5", "20");
        ShipmentAssignment assignment = assignmentService.create(
                fixture.shipment().getId(), fixture.stop().getId(), null);
        assertThrows(IllegalStateException.class,
                () -> deliveryService.completeAssignment(assignment.getId()));
        reload();

        assertEquals("ASSIGNED", assignmentService.findById(assignment.getId()).getStatus());
        assertNull(assignmentService.findById(assignment.getId()).getCompletedAt());
        assertEquals("CREATED", shipmentService.findById(fixture.shipment().getId()).getStatus());
    }

    @Test
    void routeCannotFinishWithUndeliveredShipments() {
        Fixture fixture = fixture("2.5", "20");
        assignmentService.create(fixture.shipment().getId(), fixture.stop().getId(), null);
        deliveryService.startRoute(fixture.route().getId());
        assertThrows(IllegalStateException.class,
                () -> deliveryService.completeRoute(fixture.route().getId()));
        reload();

        assertEquals("IN_PROGRESS", routeService.findById(fixture.route().getId()).getStatus());
        assertEquals("IN_TRANSIT", shipmentService.findById(fixture.shipment().getId()).getStatus());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedTransactionRollsBackWritesToAllFiveTables() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        long[] ids = new long[5];

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> transaction.executeWithoutResult(status -> {
                    Fixture fixture = fixture("2.5", "20");
                    ShipmentAssignment assignment = assignmentService.create(
                            fixture.shipment().getId(), fixture.stop().getId(), null);
                    ids[0] = fixture.user().getId();
                    ids[1] = fixture.route().getId();
                    ids[2] = fixture.shipment().getId();
                    ids[3] = fixture.stop().getId();
                    ids[4] = assignment.getId();
                    deliveryService.startRoute(fixture.route().getId());
                    entityManager.flush();
                    throw new IllegalStateException("Test rollback");
                }));

        assertEquals("Test rollback", failure.getMessage());
        for (long id : ids) {
            assertTrue(id > 0);
        }

        transaction.executeWithoutResult(status -> {
            assertNull(entityManager.find(User.class, ids[0]));
            assertNull(entityManager.find(Route.class, ids[1]));
            assertNull(entityManager.find(Shipment.class, ids[2]));
            assertNull(entityManager.find(Stop.class, ids[3]));
            assertNull(entityManager.find(ShipmentAssignment.class, ids[4]));
        });
    }

    private Fixture fixture(String weight, String maxWeight) {
        User user = userService.create("Lab 3 test");
        Route route = routeService.create(LocalDate.now(), decimal(maxWeight), null);
        Shipment shipment = shipmentService.create(user.getId(), ADDRESS, decimal(weight), null);
        Stop stop = stopService.create(route.getId(), ADDRESS, 1);
        return new Fixture(user, route, shipment, stop);
    }

    private void reload() {
        entityManager.flush();
        entityManager.clear();
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private record Fixture(User user, Route route, Shipment shipment, Stop stop) {
    }
}
