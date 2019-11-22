package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;


public class Quorum extends UntypedAbstractActor{

	// Logger attached to actor
	private final LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);

    private int pid;
    private int originalSeq;
    private int voteCount = 0;
    private int bestValue = 0;
    private int bestSeq = 0;

	// Static function creating actor
	public static Props createActor() {
		return Props.create(Quorum.class, () -> new Quorum());
	}

    public int getPid() {
        return this.pid;
    }

    private void log(String msg) {
        logger.info("["+getSelf().path().name()+"] rec msg from ["+ getSender().path().name() +"]:\n\t["+msg+"]");
    }

    public void vote(int voterid, int seq, int value) {
        this.voteCount++;
        if (seq == this.bestSeq && voterid > this.pid || seq > bestSeq) {
            bestSeq = seq;
            bestValue = value;
        }
    }

    public boolean isDecisive() {
        return this.voteCount > AkkaMain.NumActors / 2;
    }

    public int decideValue() {
        return bestValue;
    }

    public int decideSeq() { return bestSeq; }

	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof StartQuorumMessage) {
            StartQuorumMessage m = (StartQuorumMessage) message;
            this.pid = m.pid;
            this.originalSeq = m.seqToAck;
            PollMessage poll = new PollMessage(this.originalSeq);
            for (ActorRef ref : m.getPopulation()) {
                ref.tell(poll, getSelf());
            }
        } else if (message instanceof PollResponseMessage) {
            PollResponseMessage pollResp = (PollResponseMessage)message;
            // Skips out-dated responses
            if (pollResp.ack == this.originalSeq) {
                log("VOTING!");
                this.vote(pollResp.pid, pollResp.seq, pollResp.value);
                log(""+this.voteCount);
                if (this.isDecisive()) {
                    DecideQuorumMessage result = new DecideQuorumMessage(this.originalSeq, this.bestSeq, this.bestValue);
                }
            }
        } else {
            log("Message unrecognized");
        }
	}

}
