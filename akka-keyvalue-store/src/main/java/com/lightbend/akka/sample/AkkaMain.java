package com.lightbend.akka.sample;

import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AkkaMain {
    
    public final static int NumActors = 10; // N
    public final static int NumFailed = 4; // f
    public final static int NumActions = 3; // M
    /** This class shouldn't be instantiated */
    private AkkaMain() {}

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
    private static void sendListMessages(List<ActorRef> refList) {
        int i = 0;
        for (ActorRef ref : refList) {
            ListMessage listMsg = new ListMessage(i, refList);
            i++;
            ref.tell(listMsg, ActorRef.noSender());
        }
    }

    /**
     * Send LaunchMessage to all actors.
     * Some will have their fail-flag set, simulating a failed process.
     */
    private static void sendLaunchMessages(List<ActorRef> refList) {
        //Collections.shuffle(refList);
        int nFailed = 0;
        int i = 0;
        for (ActorRef ref : refList) {
            boolean failed = false;
            if (nFailed < NumFailed && i > 0) {
                failed = true;
                nFailed++;
            }
            LaunchMessage launchMsg = new LaunchMessage(failed);
            ref.tell(launchMsg, ActorRef.noSender());
            i++;
        } 
    }

    public static void main(String[] args) {

        //#actor-system
        final ActorSystem system = ActorSystem.create("helloakka");
        //#actor-system

        // Instantiate actors, populating reference list
        ArrayList<ActorRef> refList = new ArrayList<>();
        for (int i = 0; i < NumActors; i++) {
            String name = "p" + i;
            final ActorRef a = system.actorOf(Process.createActor(), name);
            refList.add(a);
        }

        // send the list of references to each actor
        sendListMessages(refList);

        //pause("Press ENTER to continue");

        // Prompting actors to start the circus
        sendLaunchMessages(refList);

        //pause("Press ENTER to exit");
        system.terminate();
    }
}
