package com.lightbend.akka.sample;

public class PollMessage {
    public int ack;
    public PollMessage(int ack) {
        this.ack = ack;
    }
}
