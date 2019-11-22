package com.lightbend.akka.sample;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

import java.util.List;
import java.util.concurrent.TimeUnit;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.lang.System;


public class Process extends UntypedAbstractActor{

	// Logger attached to actor
	private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);

    private List<ActorRef> savedList;

    private boolean isFailed;
    private int pid;

    private int value = 0;
    private int readSeq= 0;
    private int writeSeq = 0;

	// Static function creating actor
	public static Props createActor() {
		return Props.create(Process.class, Process::new);
	}

    public int getPid() {
        return this.pid;
    }

    private void log(String msg) {
        logger.info("["+getSelf().path().name()+"] rec msg from ["+ getSender().path().name() +"]:\n\t["+msg+"]");
    }

    private void formLog(boolean sending, String type, int ack, int seq, String otherActor) {
        String ackStr = ack < 0 ? "" : String.valueOf(ack);
        String[] actors = new String[2];
        // Determining actor order in the log string
        if (sending) {
            actors[0] = getSelf().path().name();
            actors[1] = otherActor;
        } else {
            actors[0] = otherActor;
            actors[1] = getSelf().path().name();
        }

        logger.info("###"+actors[0]+","+actors[1]+","+type+","+ackStr+","+seq+","+this.value+","+System.nanoTime());
    }

    private void get() {
        this.readSeq++;

        String name = "p" + this.pid + "q" + this.readSeq;
        ActorRef quorumRef = getContext().getSystem().actorOf(Quorum.createActor(), name);
        FiniteDuration duration = Duration.create(1000, TimeUnit.MILLISECONDS);
        Future<Object> future = Patterns.ask(quorumRef, new StartQuorumMessage(this.pid, this.readSeq, getSelf(), this.savedList), 1000);
        try {
            DecideQuorumMessage result = (DecideQuorumMessage) Await.result(future, duration);
            this.value = result.getValue();
            this.readSeq = result.getSeq();
            formLog(true, "set", result.ackSeq, this.readSeq, getSelf().path().name());
        } catch (Exception ex) {
            System.err.println("Bad await result");
        }

    }

    private void put(int value) {
    }

    /**
     * Let the process operate, called when successfully launched.
     * Will call multiple read/write operations in iterations.
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
            log("List of " + this.savedList.size() + " actors");

        } else if (message instanceof PollMessage) {
            PollMessage pollMsg = (PollMessage)message;
            formLog(true, "poll", pollMsg.ack, this.readSeq, getSender().path().name());
            PollResponseMessage resp = new PollResponseMessage(pollMsg.ack, this.readSeq, this.value, this.pid);
            formLog(true, "pollresp", pollMsg.ack, this.readSeq, getSender().path().name());
            getSender().tell(resp, getSelf());
        } else if (message instanceof LaunchMessage) {
            this.isFailed = ((LaunchMessage)message).failed;
            log("Launched! Fail: " + this.isFailed);
            if (! this.isFailed) {
                this.run();
            }
        } else {
            log("Message unrecognized: " + message.getClass().toString());
        }
	}

}
