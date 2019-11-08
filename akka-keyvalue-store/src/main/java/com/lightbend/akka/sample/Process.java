package com.lightbend.akka.sample;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.List;

import java.lang.System;


public class Process extends UntypedAbstractActor{

	// Logger attached to actor
	private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);

    private List<ActorRef> savedList;
    private List<Quorum> quora;

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
        //logger.info("["+getSelf().path().name()+"] rec msg from ["+ getSender().path().name() +"]:\n\t["+msg+"]");
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
        this.readSeq++;
        Quorum q = new Quorum(this.readSeq, this.pid);
        this.quora.add(q);
        PollMessage poll = new PollMessage(this.readSeq);
        for (ActorRef ref : this.savedList) {
            ref.tell(poll, getSelf());
            formLog(true, "poll", -1, this.readSeq, ref.path().name());
        }

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
            this.isFailed = ((LaunchMessage)message).failed;
            log("Launched! Fail: " + this.isFailed);
            if (! this.isFailed) {
                this.run();
            }
        } else if (message instanceof PollMessage) {
            log("Returning");
            PollMessage pollMsg = (PollMessage)message;
            formLog(false, "poll", pollMsg.ack, this.readSeq, getSender().path().name());
            PollResponseMessage resp = new PollResponseMessage(pollMsg.ack, this.readSeq, this.value, this.pid);
            formLog(true, "pollresp", pollMsg.ack, this.readSeq, getSender().path().name());
            getSender().tell(resp, getSelf());
        } else if (message instanceof PollResponseMessage) {
            PollResponseMessage pollResp = (PollResponseMessage)message;
            ListIterator<Quorum> li = this.quora.listIterator();
            while (li.hasNext()) {
                Quorum q = li.next();
                if (pollResp.ack == q.qid) {
                    q.vote(pollResp.pid, pollResp.seq, pollResp.value);
                    if (q.isDecisive()) {
                        this.readSeq = q.decideSeq();
                        this.value = q.decideValue();
                        li.remove();
                    }
                }
            formLog(false, "pollresp", pollResp.ack, this.readSeq, getSender().path().name());
            }
        } else {
            log("Message unrecognized");
        }
        // TODO: Handle responses to QueryMessage (VoteMessage?),
        // delegate to correct Quorum in this.quora.
	}

}
