package com.lightbend.akka.sample;

import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import java.io.IOException;
import java.util.ArrayList;

import com.lightbend.akka.sample.Process;
import com.lightbend.akka.sample.ListMessage;
import com.lightbend.akka.sample.PromptMessage;

public class AkkaMiniProject {

    /**
     * Prompt the user for ENTER-press with a message,
     * used for debugging purposes
     */
    private static void pause(String msg) {
        try {
            System.out.println(">>> " + msg + " <<<");
            System.in.read();
        } catch (IOException ignored) {}
    }

    /**
     * Notify each process in a list of all processes
     */
    private static void sendListMessages(ArrayList<ActorRef> refList) {
        ListMessage listMsg = new ListMessage(refList);
        for (ActorRef ref : refList) {
            ref.tell(listMsg, ActorRef.noSender());
        }
    }

    /**
     * Prompt all processes to output their knowledge of other processes
     */
    private static void sendPromptMessages(ArrayList<ActorRef> refList) {
        PromptMessage promptMsg = new PromptMessage();
        for (ActorRef ref : refList) {
            ref.tell(promptMsg, ActorRef.noSender());
        } 
    }

    public static void main(String[] args) {

        final int N = 3;

        //#actor-system
        final ActorSystem system = ActorSystem.create("helloakka");
        //#actor-system

        // Instantiate actors, populating reference list
        ArrayList<ActorRef> refList = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            String name = "a" + i;
            final ActorRef a = system.actorOf(Process.createActor(), name);
            refList.add(a);
        }

        // send the list of references to each actor
        sendListMessages(refList);

        // Remaking list to show that processes don't rely on the original list
        ArrayList<ActorRef> clonedList = new ArrayList<>();
        for (ActorRef ref : refList) {
            clonedList.add(ref);
        }
        refList.clear();

        pause("Press ENTER to continue");

        // Prompting actors to show their knowledge of other processes
        sendPromptMessages(clonedList);

        pause("Press ENTER to exit");
        system.terminate();
    }
}
