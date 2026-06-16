package com.omar.sentra.gateway.routing;

import java.util.List;

/**
 * Dry-run route validation result.
 */
public record RouteValidationResult(boolean valid, List<String> errors) {
}
