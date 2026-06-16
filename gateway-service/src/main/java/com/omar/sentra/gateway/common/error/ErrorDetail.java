package com.omar.sentra.gateway.common.error;

/**
 * One safe validation error.
 *
 * @param field request field
 * @param code machine-readable reason
 * @param message safe explanation
 */
public record ErrorDetail(String field, String code, String message) {
}
