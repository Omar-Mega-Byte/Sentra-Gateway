package com.sentra.notification.fault;

import com.sentra.notification.web.AdminTestRequest;
import com.sentra.notification.web.AdminTestResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Produces deterministic admin-only test-notification responses and fault
 * scenarios.
 */
@Service
public class AdminTestService {
    private final FaultControlService faults;
    private final Clock clock;

    /** @param faults local/test fault-control service @param clock UTC clock */
    public AdminTestService(FaultControlService faults, Clock clock) {
        this.faults = faults;
        this.clock = clock;
    }

    /** Executes the requested deterministic admin scenario. */
    public AdminTestResponse execute(HttpServletRequest request, AdminTestRequest body, String operation) {
        faults.applyAdminScenario(request, body.scenario(), operation);
        return new AdminTestResponse(body.scenario(), true, "TEST_ACCEPTED", clock.instant());
    }
}
