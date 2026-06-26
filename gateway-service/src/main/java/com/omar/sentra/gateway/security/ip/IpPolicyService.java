package com.omar.sentra.gateway.security.ip;

import com.omar.sentra.gateway.admin.PolicyRepository;
import com.omar.sentra.gateway.common.error.ErrorCode;
import com.omar.sentra.gateway.common.error.GatewayException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Evaluates the route-selected IP rule.
 */
@Service
public class IpPolicyService {
    private final PolicyRepository repository;

    public IpPolicyService(PolicyRepository repository) {
        this.repository = repository;
    }

    public Mono<Void> enforce(String policyId, String routeId, String clientIp) {
        if (policyId == null) {
            return Mono.empty();
        }
        Instant now = Instant.now();
        return repository.findIpRule(policyId)
                .filter(IpRule::enabled)
                .filter(rule -> !now.isBefore(rule.validFrom()))
                .filter(rule -> rule.expiresAt() == null || now.isBefore(rule.expiresAt()))
                .filter(rule -> rule.routeId() == null || rule.routeId().equals(routeId))
                .filter(rule -> CidrMatcher.matches(rule.network(), clientIp))
                .flatMap(rule -> "ALLOW".equals(rule.action())
                        ? Mono.empty()
                        : Mono.error(new GatewayException(ErrorCode.GW_IP_DENIED)))
                .then();
    }
}
