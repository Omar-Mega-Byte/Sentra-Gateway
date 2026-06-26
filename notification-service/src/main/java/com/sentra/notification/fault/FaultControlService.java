package com.sentra.notification.fault;

import com.sentra.notification.common.error.ErrorCode;
import com.sentra.notification.common.error.ServiceException;
import com.sentra.notification.common.request.SentraHeaders;
import com.sentra.notification.config.NotificationServiceProperties;
import com.sentra.notification.observability.NotificationMetrics;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Applies bounded local/test-only fault controls for gateway resilience tests.
 */
@Service
public class FaultControlService {
    private final NotificationServiceProperties properties;
    private final Environment environment;
    private final NotificationMetrics metrics;

    /** @param properties service configuration @param environment active environment @param metrics bounded metrics */
    public FaultControlService(NotificationServiceProperties properties, Environment environment, NotificationMetrics metrics) {
        this.properties = properties;
        this.environment = environment;
        this.metrics = metrics;
    }

    /** Applies documented local/test fault-control headers to an internal operation. */
    public void applyHeaderFaults(HttpServletRequest request, String operation) {
        applyDelayHeader(request, operation);
        applyStatusHeader(request, operation);
        if (truthy(request.getHeader(SentraHeaders.TEST_MALFORMED))) {
            ensureAllowed(operation, "malformed", properties.fault().allowMalformed());
            metrics.recordFault(operation, "malformed", "committed");
            throw new MalformedResponseException(200);
        }
        if (truthy(request.getHeader(SentraHeaders.TEST_DISCONNECT))) {
            ensureAllowed(operation, "disconnect", properties.fault().allowDisconnect());
            metrics.recordFault(operation, "disconnect", "committed");
            throw new DisconnectSimulationException();
        }
    }

    /** Applies the scenario selected by the admin request body. */
    public void applyAdminScenario(HttpServletRequest request, TestScenario scenario, String operation) {
        switch (scenario) {
            case SUCCESS -> {
            }
            case DELAY -> {
                ensureAllowed(operation, "delay", properties.fault().allowDelay());
                delay(Math.min(Math.max(1, properties.fault().maxDelayMs()), 100), operation, "scenario-delay");
            }
            case FAILURE -> {
                ensureAllowed(operation, "status", properties.fault().allowStatus());
                int status = statusFromHeaderOrDefault(request, 500);
                metrics.recordFault(operation, "status", "failure-" + status);
                throw ServiceException.of(ErrorCode.NTF_TEST_FAILURE, status);
            }
            case MALFORMED -> {
                ensureAllowed(operation, "malformed", properties.fault().allowMalformed());
                metrics.recordFault(operation, "malformed", "committed");
                throw new MalformedResponseException(200);
            }
            case DISCONNECT -> {
                ensureAllowed(operation, "disconnect", properties.fault().allowDisconnect());
                metrics.recordFault(operation, "disconnect", "committed");
                throw new DisconnectSimulationException();
            }
        }
    }

    private void applyDelayHeader(HttpServletRequest request, String operation) {
        String header = request.getHeader(SentraHeaders.TEST_DELAY_MILLIS);
        if (header == null) {
            return;
        }
        ensureAllowed(operation, "delay", properties.fault().allowDelay());
        int delayMs = parseInteger(header);
        if (delayMs < 0 || delayMs > properties.fault().maxDelayMs()) {
            throw ServiceException.of(ErrorCode.NTF_REQUEST_INVALID);
        }
        delay(delayMs, operation, "header-delay");
    }

    private void applyStatusHeader(HttpServletRequest request, String operation) {
        String header = request.getHeader(SentraHeaders.TEST_STATUS);
        if (header == null) {
            return;
        }
        ensureAllowed(operation, "status", properties.fault().allowStatus());
        int status = parseInteger(header);
        if (!properties.fault().allowedStatuses().contains(status)) {
            throw ServiceException.of(ErrorCode.NTF_REQUEST_INVALID);
        }
        metrics.recordFault(operation, "status", "failure-" + status);
        throw ServiceException.of(ErrorCode.NTF_TEST_FAILURE, status);
    }

    private int statusFromHeaderOrDefault(HttpServletRequest request, int defaultStatus) {
        String header = request.getHeader(SentraHeaders.TEST_STATUS);
        if (header == null) {
            return defaultStatus;
        }
        int status = parseInteger(header);
        if (!properties.fault().allowedStatuses().contains(status)) {
            throw ServiceException.of(ErrorCode.NTF_REQUEST_INVALID);
        }
        return status;
    }

    private int parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw ServiceException.of(ErrorCode.NTF_REQUEST_INVALID);
        }
    }

    private void delay(int delayMs, String operation, String result) {
        metrics.recordFault(operation, "delay", result);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw ServiceException.of(ErrorCode.NTF_INTERNAL_ERROR);
        }
    }

    private void ensureAllowed(String operation, String fault, boolean featureAllowed) {
        if (!properties.fault().controlsEnabled() || !properties.localOrTest(List.of(environment.getActiveProfiles())) || !featureAllowed) {
            metrics.recordFault(operation, fault, "disabled");
            throw ServiceException.of(ErrorCode.NTF_FAULT_CONTROL_DISABLED);
        }
    }

    private boolean truthy(String value) {
        return value != null && ("true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()));
    }
}
