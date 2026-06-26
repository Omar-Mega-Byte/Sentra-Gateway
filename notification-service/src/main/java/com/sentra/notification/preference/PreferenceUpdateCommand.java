package com.sentra.notification.preference;

/**
 * Validated preference update command carrying client-visible settings and the
 * expected optimistic version.
 *
 * @param emailEnabled requested email preference
 * @param smsEnabled requested SMS preference
 * @param pushEnabled requested push preference
 * @param webhookEnabled requested webhook preference
 * @param expectedVersion optimistic version supplied by the caller
 */
public record PreferenceUpdateCommand(boolean emailEnabled, boolean smsEnabled, boolean pushEnabled, boolean webhookEnabled, int expectedVersion) {
}
