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
                case "put":
                    this.operations.addFirst(new Operation("guaranteedWrite", op.operand));
                    this.get();
                    break;
                case "guaranteedWrite":
                    this.put(op.operand);
                    break;
                case "get":
                    this.operations.addFirst(new Operation("notifyWrite", op.operand));
                    this.get();
                    break;
                case "notifyWrite":
                    this.put(this.value);
                    break;
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
        this.quorum.vote(this.pid, this.readSeq, this.value);
        formLog(true, "startpoll", -1, this.readSeq, getSelf().path().name());
        for (ActorRef ref : this.savedList) {
            if (ref != getSelf())
                ref.tell(poll, getSelf());
        }
    }

    /**
     * This method is guaranteed to be run after a get() has just been performed
     * @param newValue
     */
    private void put(int newValue) {
        WriteMessage notice = new WriteMessage(this.pid, this.readSeq, newValue);
        this.quorum = new Quorum(this.pid, this.readSeq);
        this.quorum.vote(this.pid, this.readSeq, newValue);
        formLog(true, "startwrite", -1, this.readSeq, getSelf().path().name());
        for (ActorRef ref : this.savedList) {
            if (ref != getSelf())
                ref.tell(notice, getSelf());
        }
    }

    /**
     * Let the process operate, called when successfully launched.
     * Will call multiple read/write operations in iterations.
     */
    private void run() {
        this.operations.add(new Operation("put", 100 + this.pid));
        this.operations.add(new Operation("get", -1 /* unused */));
        this.consumeOperation();
    }


	@Override
	public void onReceive(Object message) throws Throwable {
        if (this.isFailed) {
            return;
        }
        if (message instanceof ListMessage) {
            ListMessage m = (ListMessage) message;
            this.pid = m.pid;
            this.savedList = m.getList();

        } else if (message instanceof PollMessage) {
            PollMessage pollMsg = (PollMessage)message;
            PollResponseMessage resp = new PollResponseMessage(pollMsg.ack, this.readSeq, this.value, this.pid);
            getSender().tell(resp, getSelf());
        } else if (message instanceof PollResponseMessage) {
            PollResponseMessage pollResp = (PollResponseMessage)message;
            // Skips out-dated responses
            if (null != this.quorum && pollResp.ack == this.readSeq) {
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
        } else if (message instanceof WriteMessage) {
            WriteMessage writeMsg = (WriteMessage)message;

            if (this.isFailed) {
                return;
            }
            if (writeMsg.seq > this.readSeq || (this.readSeq == writeMsg.seq && writeMsg.pid > this.pid)) {
                this.readSeq = writeMsg.seq;
                this.value = writeMsg.value;
            }
            WriteResponseMessage resp = new WriteResponseMessage(this.pid, writeMsg.seq, writeMsg.value);
            getSender().tell(resp, getSelf());

        } else if (message instanceof WriteResponseMessage) {
            WriteResponseMessage writeResp = (WriteResponseMessage)message;
            // Skips out-dated responses
            if (null != this.quorum && writeResp.seq== this.readSeq) {
                this.quorum.vote(writeResp.pid, writeResp.seq, writeResp.value);
                if (this.quorum.isDecisive()) {
                    this.value = this.quorum.decideValue();
                    this.readSeq = this.quorum.decideSeq();
                    this.quorum = null;
                    formLog(true, "writeset", writeResp.seq, this.readSeq, getSelf().path().name());
                    // Continue processing
                    if (! this.operations.isEmpty()) {
                        consumeOperation();
                    }
                }
            } else {
                log("Could not handle WriteResponse, what's this?");
                log(writeResp.seq + " " + writeResp.value + ", compared to : "+this.readSeq + " but my quorum is " + (null != this.quorum ? this.quorum.originalSeq : "null"));
            }

        } else if (message instanceof LaunchMessage) {
            this.isFailed = ((LaunchMessage)message).failed;
            if (! this.isFailed) {
                this.operations = new ArrayDeque<>();
                this.run();
            }
        } else {
            log("Message unrecognized: " + message.getClass().toString());
        }
	}

}
