package com.lightbend.akka.sample;

public class WriteResponseMessage {
    public final int pid;
    public final int ack;
    public final int seq;
    public final int value;

    public WriteResponseMessage(int pid, int ack, int seq, int value) {
        this.pid = pid;
        this.ack = ack;
        this.seq = seq;
        this.value = value;
    }

}
