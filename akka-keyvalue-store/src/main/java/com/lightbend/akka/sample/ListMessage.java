package com.lightbend.akka.sample;
import akka.actor.ActorRef;
import java.util.List;
import java.util.ArrayList;

public class ListMessage {
    private List<ActorRef> list;
    public int pid;
    public ListMessage(int pid, List<ActorRef> list) {
        this.pid = pid;
        // Cloning list by iterating
        this.list = new ArrayList<>();
        for (ActorRef ref : list) {
            this.list.add(ref);
        }
    }

    public List<ActorRef> getList() {
        return this.list;
    }

}
