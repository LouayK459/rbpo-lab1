-- Выполнить один раз после создания таблиц.
WITH new_user AS (
    INSERT INTO delivery.users (name)
    VALUES ('Louay')
    RETURNING id
),
new_route AS (
    INSERT INTO delivery.routes (route_date, max_weight)
    VALUES (CURRENT_DATE, 20)
    RETURNING id
),
new_shipment AS (
    INSERT INTO delivery.shipments (sender_id, address, weight)
    SELECT id, 'Moscow, Aviamotornaya 8', 2.5
    FROM new_user
    RETURNING id
),
new_stop AS (
    INSERT INTO delivery.stops (route_id, address, sequence_number)
    SELECT id, 'Moscow, Aviamotornaya 8', 1
    FROM new_route
    RETURNING id
),
new_assignment AS (
    INSERT INTO delivery.shipment_assignments (shipment_id, stop_id)
    SELECT new_shipment.id, new_stop.id
    FROM new_shipment CROSS JOIN new_stop
    RETURNING id
)
SELECT
    (SELECT id FROM new_user) AS user_id,
    (SELECT id FROM new_route) AS route_id,
    (SELECT id FROM new_shipment) AS shipment_id,
    (SELECT id FROM new_stop) AS stop_id,
    (SELECT id FROM new_assignment) AS assignment_id;
