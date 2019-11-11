package com.lightbend.akka.sample;

public class Quorum {

    // These are changed by votes
    private int bestSeq = -1;
    private int bestValue = 0;

    private int voteCount = 0;

    // ID to reference this poll
    public final int qid;

    // ID of the governing process
    public final int pid;

    public Quorum(int qid, int pid) {
        this.qid = qid;
        this.pid = pid;
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

    public int decideSeq() {
        return bestSeq;
    }
}
