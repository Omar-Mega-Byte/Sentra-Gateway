package com.sentra.notification.fault;

/**
 * Internal signal for a local/test disconnect-style partial response.
 */
public class DisconnectSimulationException extends RuntimeException {
    /** Creates the disconnect simulation signal. */
    public DisconnectSimulationException() {
        super("Disconnect simulation");
    }
}
