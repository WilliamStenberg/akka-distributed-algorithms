package com.lightbend.akka.sample;

public class NotifyMessage {
    public int value;
    public int readSeq;
    public NotifyMessage(int value, int readSeq) {
        this.value = value;
        this.readSeq = readSeq;
    }
}
