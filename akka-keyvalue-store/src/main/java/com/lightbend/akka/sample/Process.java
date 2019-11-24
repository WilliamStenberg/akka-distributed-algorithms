package com.lightbend.akka.sample;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.*;

import java.lang.System;


public class Process extends UntypedAbstractActor{

    private class Operation {
        private final String opName;
        private final int operand;

        public Operation(String opName, int operand) {
            this.opName = opName;
            this.operand = operand;
        }
    }

	// Logger attached to actor
	private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);

    private List<ActorRef> savedList;
    private Deque<Operation> operations;
    private Quorum quorum = null;

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

    private void consumeOperation() {
	    Operation op = this.operations.getFirst();
	    if (null != op) {
	        this.operations.removeFirst();
            switch (op.opName) {
                case "write":
                    // TODO implement
                case "read":
                default:
                    this.get();
            }
        }
    }

    private void get() {
        this.readSeq++;

        String name = "p" + this.pid + "q" + this.readSeq;

        PollMessage poll = new PollMessage(this.readSeq);
        this.quorum = new Quorum(this.pid, this.readSeq);
        this.quorum.vote(this.pid, this.readSeq-1, this.value);
        formLog(true, "startpoll", -1, this.readSeq, getSelf().path().name());
        for (ActorRef ref : this.savedList) {
            if (ref != getSelf())
                ref.tell(poll, getSelf());
        }
    }

    private void put(int value) {
    }

    /**
     * Let the process operate, called when successfully launched.
     * Will call multiple read/write operations in iterations.
     */
    private void run() {
        log("Running boy "+this.pid);
        this.operations.add(new Operation("read", -1 /* unused */));
        this.consumeOperation();
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

            if (this.isFailed) {
                return;
            }
            PollResponseMessage resp = new PollResponseMessage(pollMsg.ack, this.readSeq, this.value, this.pid);
            getSender().tell(resp, getSelf());
        } else if (message instanceof PollResponseMessage) {
            PollResponseMessage pollResp = (PollResponseMessage)message;
            // Skips out-dated responses
            if (null != this.quorum && pollResp.ack == this.readSeq) {
                formLog(true, "vote", pollResp.ack, this.readSeq, getSender().path().name());
                this.quorum.vote(pollResp.pid, pollResp.seq, pollResp.value);
                if (this.quorum.isDecisive()) {
                    this.value = this.quorum.decideValue();
                    this.readSeq = this.quorum.decideSeq();
                    this.quorum = null;
                    formLog(true, "set", pollResp.ack, this.readSeq, getSelf().path().name());

                    // Continue processing
                    if (! this.operations.isEmpty()) {
                        consumeOperation();
                    }
                }
            }
        } else if (message instanceof LaunchMessage) {
            this.isFailed = ((LaunchMessage)message).failed;
            log("Launched! Fail: " + this.isFailed);
            if (! this.isFailed) {
                this.operations = new ArrayDeque<>();
                this.run();
            }
        } else {
            log("Message unrecognized: " + message.getClass().toString());
        }
	}

}
