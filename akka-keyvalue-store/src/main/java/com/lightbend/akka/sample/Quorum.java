package com.lightbend.akka.sample;

public class Quorum {

    private int pid;
    public int originalSeq;
    public int originalValue;
    //TODO change line below to private, only for outside Logging
    public int voteCount = 0;
    private int bestValue = 0;
    private int bestSeq = -1;

    public Quorum(int pid, int originalSeq) {
        this.pid = pid;
        this.originalSeq = originalSeq;
        this.originalValue = -1;
    }
    public Quorum(int pid, int originalSeq, int originalValue) {
        this.pid = pid;
        this.originalSeq = originalSeq;
        this.originalValue = originalValue;
    }

    public int getPid() {
        return this.pid;
    }

    public void vote(int voterid, int seq, int value) {
        this.voteCount++;
        if (seq == this.bestSeq && voterid > this.pid || seq > bestSeq) {
            bestSeq = seq;
            bestValue = value;
        }
    }

    public boolean isDecisive() {
        return this.voteCount > AkkaMain.NumActors / 2;
    }

    public int decideValue() {
        return bestValue;
    }

    public int decideSeq() { return bestSeq; }

}

