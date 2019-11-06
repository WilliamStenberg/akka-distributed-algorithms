package com.lightbend.akka.sample;

import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import java.io.IOException;
import java.util.ArrayList;

import com.lightbend.akka.sample.Process;
import com.lightbend.akka.sample.ListMessage;
import com.lightbend.akka.sample.LaunchMessage;

public class AkkaMain {

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
     * Send LaunchMessage to all actors.
     * Some will have their fail-flag set, simulating a failed process.
     */
    private static void sendLaunchMessages(ArrayList<ActorRef> refList) {
        for (ActorRef ref : refList) {
            boolean failed = Math.random() > 0.7;
            LaunchMessage launchMsg = new LaunchMessage(failed);
            ref.tell(launchMsg, ActorRef.noSender());
        } 
    }

    public static void main(String[] args) {

        final int N = 10;

        //#actor-system
        final ActorSystem system = ActorSystem.create("helloakka");
        //#actor-system

        // Instantiate actors, populating reference list
        ArrayList<ActorRef> refList = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            String name = "p" + i;
            final ActorRef a = system.actorOf(Process.createActor(), name);
            refList.add(a);
        }

        // send the list of references to each actor
        sendListMessages(refList);

        pause("Press ENTER to continue");

        // Prompting actors to start the circus
        sendLaunchMessages(refList);

        pause("Press ENTER to exit");
        system.terminate();
    }
}
