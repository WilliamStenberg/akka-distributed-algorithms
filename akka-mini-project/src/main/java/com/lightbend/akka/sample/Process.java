package com.lightbend.akka.sample;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;

import com.lightbend.akka.sample.ListMessage;
import com.lightbend.akka.sample.WelcomeMessage;
import com.lightbend.akka.sample.PromptMessage;

public class Process extends UntypedAbstractActor{

	// Logger attached to actor
	private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);

    private ArrayList<ActorRef> savedList;

	public Process() {}

	// Static function creating actor
	public static Props createActor() {
		return Props.create(Process.class, () -> {
			return new Process();
		});
	}

    private void log(final String msg) {
        logger.info("["+getSelf().path().name()+"] received message from ["+ getSender().path().name() +"] with data:\n\t["+msg+"]");
    }


	@Override
	public void onReceive(Object message) throws Throwable {
		if(message instanceof WelcomeMessage){
			WelcomeMessage m = (WelcomeMessage) message;
            log(m.getMessage());
		} else if (message instanceof ListMessage) {
            ListMessage m = (ListMessage) message;
            this.savedList = m.getList();
            log("ListMessage, sending " + this.savedList.size() + " welcomes");
            WelcomeMessage welMsg = new WelcomeMessage("Welcome");
            for (ActorRef ref : this.savedList) {
                ref.tell(welMsg, this.getSelf());
            }
        } else if (message instanceof PromptMessage) {
            StringBuilder commaSeparatedProcesses = new StringBuilder();
            for (ActorRef ref : this.savedList) {
                commaSeparatedProcesses.append(ref.path().name() + ", ");
            }
            // Chopping last ", "
            int len = commaSeparatedProcesses.length();
            commaSeparatedProcesses.delete(len-2, len);
            log("PromptMessage, am aware of " + this.savedList.size() + " processes (including self): " + commaSeparatedProcesses);
        } else {
            log("Message unrecognized");
        }
	}

}
