package com.lightbend.akka.sample;

public class WelcomeMessage {
    private final String msg;
    public WelcomeMessage(final String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return this.msg;
    }
}
