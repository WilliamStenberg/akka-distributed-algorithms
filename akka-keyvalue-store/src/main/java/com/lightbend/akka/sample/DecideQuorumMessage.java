package com.lightbend.akka.sample;

public class DecideQuorumMessage {
    private int seq;
    private int value;
    public int ackSeq;


    public DecideQuorumMessage(int ackSeq, int seq, int value) {
        this.ackSeq = ackSeq;
        this.seq = seq;
        this.value = value;
    }

    public int getSeq() {
        return this.seq;
    }

    public int getValue() {
        return this.value;
    }

}
