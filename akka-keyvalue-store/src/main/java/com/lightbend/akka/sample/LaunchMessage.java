package com.lightbend.akka.sample;

/**
 * Launch message to prompt the processes
 * to start sending (and receiving) messages.
 * Also holds failure status to simulate dead/hanging processes.
 */
public class LaunchMessage {
    public boolean failed;
    public LaunchMessage(boolean failed) {
        this.failed = failed;
    }
}
