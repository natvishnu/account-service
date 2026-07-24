package com.eventledger.account.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight health endpoint required by the brief: reports service status plus a real
 * database connectivity probe. Returns 200 when healthy, 503 when the DB is unreachable.
 */
@RestController
public class HealthController {

    private final DataSource dataSource;
    private final Clock clock;
    private final String serviceName;

    public HealthController(DataSource dataSource,
                            Clock clock,
                            @Value("${spring.application.name}") String serviceName) {
        this.dataSource = dataSource;
        this.clock = clock;
        this.serviceName = serviceName;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp = isDatabaseUp();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", dbUp ? "UP" : "DOWN");
        body.put("service", serviceName);
        body.put("timestamp", clock.instant().toString());

        Map<String, Object> db = new LinkedHashMap<>();
        db.put("status", dbUp ? "UP" : "DOWN");
        body.put("database", db);

        return ResponseEntity.status(dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean isDatabaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
