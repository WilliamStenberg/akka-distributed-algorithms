package com.lightbend.akka.sample;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;
import java.util.List;


public class Process extends UntypedAbstractActor{

	// Logger attached to actor
	private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);

    private List<ActorRef> savedList;
    private List<Quorum> quora;

    private boolean is_failed;
    private int pid;

    private int value = 0;
    private int read_seq= 0;
    private int write_seq = 0;
    private int quorum_seq = 0;

	// Static function creating actor
	public static Props createActor() {
		return Props.create(Process.class, () -> {
			return new Process();
		});
	}

    private void log(final String msg) {
        logger.info("["+getSelf().path().name()+"] rec msg from ["+ getSender().path().name() +"]:\n\t["+msg+"]");
    }

    private void get() {
        // Start a new quorum poll
        Quorum q = new Quorum(this.quorum_seq, this.pid);
        this.quora.add(q);
        this.quorum_seq++;
        // Send out messages referencing this poll
        // TODO: QueryMessage

        // Quorum will receive messages and decide final result

    }

    private void put(int value) {
    }

    /**
     * Let the process operate, called when successfully launched.
     * Will cann multiple read/write operations in iterations.
     */
    private void run() {
        this.get();
    }


	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof ListMessage) {
            ListMessage m = (ListMessage) message;
            this.pid = m.pid;
            this.savedList = m.getList();
            this.quora = new ArrayList<>();
            log("List of " + this.savedList.size() + " actors");
        } else if (message instanceof LaunchMessage) {
            this.is_failed = ((LaunchMessage)message).failed;
            log("Launched! Fail: " + this.is_failed);
            if (! this.is_failed) {
                this.run();
            }
        } else {
            log("Message unrecognized");
        }
        // TODO: Handle responses to QueryMessage (VoteMessage?),
        // delegate to correct Quorum in this.quora.
	}

}
