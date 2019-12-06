package com.lightbend.akka.sample;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.*;

import java.lang.System;


public class Process extends UntypedAbstractActor{

    private static class Operation {
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

	// Static function creating actor
	public static Props createActor() {
		return Props.create(Process.class, Process::new);
	}

    private void formLog(String type, int ack, int seq, int value, String otherActor) {
	    long nano = System.nanoTime();
        String ackStr = ack < 0 ? "" : String.valueOf(ack);

        logger.info("###"+getSelf().path().name()+","+otherActor+","+type+","+ackStr+","+seq+","+value+","+nano);
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
                    this.readSeq++;
                    this.value = op.operand;
                    this.put(op.operand);
                    break;
                case "get":
                    this.operations.addFirst(new Operation("notifyWrite", -1));
                    this.get();
                    break;
                case "notifyWrite":
                    this.readSeq++;
                    this.put(this.value);
                    break;
                default:
                    this.get();
            }
        }
    }

    private void get() {
        PollMessage poll = new PollMessage(this.readSeq);
        this.quorum = new Quorum(this.pid, this.readSeq);
        this.quorum.vote(this.pid, this.readSeq, this.value);
        for (ActorRef ref : this.savedList) {
            if (ref != getSelf())
                ref.tell(poll, getSelf());
        }
    }

    /**
     * This method is guaranteed to be run after a get() has just been performed
     */
    private void put(int newValue) {
        WriteMessage notice = new WriteMessage(this.pid, this.readSeq, newValue);
        this.quorum = new Quorum(this.pid, this.readSeq, newValue);
        this.quorum.vote(this.pid, this.readSeq, newValue);
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
        formLog("startprocess", this.pid, this.pid, this.pid, getSelf().path().name());
        for (int i = 0; i < AkkaMain.NumActions; i++) {
            this.operations.add(new Operation("put", ((i + 1) * 100)));
        }
        for (int i = 0; i < AkkaMain.NumActions; i++) {
            this.operations.add(new Operation("get", -1 /* unused */));
        }
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
            if (null != this.quorum && pollResp.ack == this.quorum.originalSeq) {
                this.quorum.vote(pollResp.pid, pollResp.seq, pollResp.value);
                if (this.quorum.isDecisive()) {
                    this.value = this.quorum.decideValue();
                    this.readSeq = this.quorum.decideSeq();
                    this.quorum = null;

                    // Continue processing
                    if (! this.operations.isEmpty()) {
                        consumeOperation();
                    }
                }
            }
        } else if (message instanceof WriteMessage) {
            WriteMessage writeMsg = (WriteMessage)message;

            if (writeMsg.seq > this.readSeq || (this.readSeq == writeMsg.seq && writeMsg.pid > this.pid)) {
                this.readSeq = writeMsg.seq;
                this.value = writeMsg.value;
            }
            WriteResponseMessage resp = new WriteResponseMessage(this.pid, writeMsg.seq, this.readSeq, this.value);
            getSender().tell(resp, getSelf());

        } else if (message instanceof WriteResponseMessage) {
            WriteResponseMessage writeResp = (WriteResponseMessage)message;
            // Skips out-dated responses
            if (null != this.quorum && writeResp.ack == this.quorum.originalSeq) {
                this.quorum.vote(writeResp.pid, writeResp.seq, writeResp.value);
                if (this.quorum.isDecisive()) {
                    int temp = this.value;
                    this.value = this.quorum.originalValue;
                    this.value = temp;
                    this.quorum = null;
                    // Continue processing
                    if (! this.operations.isEmpty()) {
                        consumeOperation();
                    } else {
                        formLog("endprocess", this.pid, this.readSeq, this.value, getSelf().path().name());
                    }
                }
            } else {
                System.err.println("Could not handle WriteResponse, what's this?");
            }

        } else if (message instanceof LaunchMessage) {
            this.isFailed = ((LaunchMessage)message).failed;
            if (! this.isFailed) {
                this.operations = new ArrayDeque<>();
                this.run();
            }
        } else {
            System.err.println("Message unrecognized: " + message.getClass().toString());
        }
	}

}
