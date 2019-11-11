package com.lightbend.akka.sample;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;
import java.util.List;

import java.lang.System;


public class Process extends UntypedAbstractActor{

	// Logger attached to actor
	private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);

    private List<ActorRef> savedList;
    private Quorum quorum = null;

    private boolean isFailed;
    private int pid;

    private int value = 0;
    private int readSeq= 0;
    private int writeSeq = 0;

	// Static function creating actor
	public static Props createActor() {
		return Props.create(Process.class, () -> {
			return new Process();
		});
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
        // Start a new quorum poll
        // Send out messages referencing this poll
        // TODO: QueryMessage
        System.out.println("In get");
        this.readSeq++;
        this.quorum = new Quorum(this.readSeq, this.pid);
        PollMessage poll = new PollMessage(this.readSeq);
        for (ActorRef ref : this.savedList) {
            ref.tell(poll, getSelf());
            System.out.println("S");
            formLog(true, "startpoll", -1, this.readSeq, ref.path().name());
        }
        if (this.quorum.isDecisive()) {
        }
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
            log("List of " + this.savedList.size() + " actors");
        } else if (message instanceof LaunchMessage) {
            this.isFailed = ((LaunchMessage)message).failed;
            log("Launched! Fail: " + this.isFailed);
            if (! this.isFailed) {
                this.run();
            }
        } else if (message instanceof PollMessage) {
            PollMessage pollMsg = (PollMessage)message;
            formLog(true, "poll", pollMsg.ack, this.readSeq, getSender().path().name());
            PollResponseMessage resp = new PollResponseMessage(pollMsg.ack, this.readSeq, this.value, this.pid);
            formLog(true, "pollresp", pollMsg.ack, this.readSeq, getSender().path().name());
            getSender().tell(resp, getSelf());
        } else if (message instanceof PollResponseMessage) {
            PollResponseMessage pollResp = (PollResponseMessage)message;
            formLog(false, "vote", pollResp.ack, this.readSeq, getSender().path().name());
            if (null != this.quorum && pollResp.ack == this.quorum.qid) {
                log("Voting");
                this.quorum.vote(pollResp.pid, pollResp.seq, pollResp.value);
                if (this.quorum.isDecisive()) {
                    this.value = this.quorum.decideValue();
                    this.readSeq = this.quorum.decideSeq();
                    formLog(true, "set", pollResp.ack, this.readSeq, getSender().path().name());
                    this.quorum = null;
                }
            }
        } else {
            log("Message unrecognized");
        }
	}

}
