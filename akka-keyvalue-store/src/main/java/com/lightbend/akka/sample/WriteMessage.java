package com.lightbend.akka.sample;

public class WriteMessage {
    public final int seq;
    public final int value;
    public final int pid;
    public WriteMessage(int pid, int seq, int value) {
        this.pid = pid;
        this.seq = seq;
        this.value = value;
    }
}
