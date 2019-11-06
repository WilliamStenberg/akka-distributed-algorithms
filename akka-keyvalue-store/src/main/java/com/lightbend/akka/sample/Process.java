package com.lightbend.akka.sample;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;

import com.lightbend.akka.sample.ListMessage;
import com.lightbend.akka.sample.LaunchMessage;

public class Process extends UntypedAbstractActor{

	// Logger attached to actor
	private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);

    private ArrayList<ActorRef> savedList;

    private boolean is_failed;

	public Process() {}

	// Static function creating actor
	public static Props createActor() {
		return Props.create(Process.class, () -> {
			return new Process();
		});
	}

    private void log(final String msg) {
        logger.info("["+getSelf().path().name()+"] rec msg from ["+ getSender().path().name() +"]:\n\t["+msg+"]");
    }


	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof ListMessage) {
            ListMessage m = (ListMessage) message;
            this.savedList = m.getList();
            log("List of " + this.savedList.size() + " actors");
        } else if (message instanceof LaunchMessage) {
            this.is_failed = ((LaunchMessage)message).failed;
            log("Launched! Fail: " + this.is_failed);
        } else {
            log("Message unrecognized");
        }
	}

}
