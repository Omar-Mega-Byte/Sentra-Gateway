package com.omar.sentra.gateway.security.risk;

import com.omar.sentra.gateway.admin.PolicyRepository;
import com.omar.sentra.gateway.common.error.ErrorCode;
import com.omar.sentra.gateway.common.error.GatewayException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Evaluates bounded request-shape risk signals.
 */
@Service
public class RiskService {
    private final PolicyRepository repository;

    public RiskService(PolicyRepository repository) {
        this.repository = repository;
    }

    public Mono<String> evaluate(String ruleId, String routeId, ServerWebExchange exchange) {
        if (ruleId == null) {
            return Mono.just("ALLOW");
        }
        return repository.findRiskRule(ruleId)
                .filter(RiskRule::enabled)
                .filter(rule -> rule.routeId() == null || rule.routeId().equals(routeId))
                .flatMap(rule -> signal(rule.signal(), exchange) >= rule.thresholdValue()
                        ? action(rule.action())
                        : Mono.just("ALLOW"))
                .defaultIfEmpty("ALLOW");
    }

    private int signal(String signal, ServerWebExchange exchange) {
        return switch (signal) {
            case "HEADER_COUNT" -> exchange.getRequest().getHeaders().size();
            case "QUERY_PARAMETER_COUNT" -> exchange.getRequest().getQueryParams().size();
            case "PATH_SEGMENTS" -> exchange.getRequest().getPath().elements().size();
            default -> 0;
        };
    }

    private Mono<String> action(String action) {
        return switch (action) {
            case "DENY", "TEMP_BLOCK" -> Mono.error(new GatewayException(ErrorCode.GW_RISK_DENIED));
            case "THROTTLE" -> Mono.just("THROTTLE");
            default -> Mono.just("OBSERVE");
        };
    }
}
