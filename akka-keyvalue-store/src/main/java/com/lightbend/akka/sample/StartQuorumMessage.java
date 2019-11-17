package com.lightbend.akka.sample;
import akka.actor.ActorRef;

import java.util.List;

public class StartQuorumMessage {
    public final ActorRef ref;
    public final List<ActorRef> population;
    public int pid;
    public int seqToAck;
    public StartQuorumMessage(int pid, int seq, ActorRef ref, List<ActorRef> population) {
        this.pid = pid;
        this.ref = ref;
        this.seqToAck = seq;
        this.population = population;
    }

    public List<ActorRef> getPopulation() {
        return this.population;
    }
}
