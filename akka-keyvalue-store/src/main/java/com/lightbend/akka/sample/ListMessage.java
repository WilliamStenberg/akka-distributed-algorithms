package com.lightbend.akka.sample;
import akka.actor.ActorRef;
import java.util.ArrayList;

public class ListMessage {
    private ArrayList<ActorRef> list;
    public ListMessage(ArrayList<ActorRef> list) {
        // Cloning list by iterating
        this.list = new ArrayList<>();
        for (ActorRef ref : list) {
            this.list.add(ref);
        }
    }

    public ArrayList<ActorRef> getList() {
        return this.list;
    }

}
