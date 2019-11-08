package com.lightbend.akka.sample;

public class PollResponseMessage {
    public int ack;
    public int seq;
    public int value;
    public int pid;

    public PollResponseMessage(int ack, int seq, int value, int pid) {
        this.ack = ack;
        this.seq = seq;
        this.value = value;
        this.pid = pid;
    }
}
