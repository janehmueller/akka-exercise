package de.hpi.akka_exercise.remote.actors;

import akka.actor.*;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Reaper extends AbstractLoggingActor {
    public static final String DEFAULT_NAME = "reaper";
    private final Set<ActorRef> watchees = new HashSet<>();

    public static Props props() { return Props.create(Reaper.class); }

    @NoArgsConstructor
    public static class WatchMeMessage implements Serializable {}

    public static void watchWithDefaultReaper(AbstractActor actor) {
        ActorSelection defaultReaper = actor.getContext().getSystem().actorSelection("/user/" + DEFAULT_NAME);
        defaultReaper.tell(new WatchMeMessage(), actor.getSelf());
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        log().info("Started {}...", this.getSelf());
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        log().info("Stopped {}.", this.getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(WatchMeMessage.class, this::handle)
            .match(Terminated.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
    }

    private void handle(WatchMeMessage message) {
        final ActorRef sender = this.getSender();
        if(this.watchees.add(sender)) {
            this.getContext().watch(sender);
            this.log().info("Started watching {}.", sender);
        }
    }

    private void handle(Terminated message) {
        final ActorRef sender = this.getSender();
        if(this.watchees.remove(sender)) {
            this.log().info("Reaping {}.", sender);
            if(this.watchees.isEmpty()) {
                this.log().info("Every local actor has been reaped. Terminating the actor system...");
                this.getContext().getSystem().terminate();
            }
        } else {
            this.log().error("Got termination message from unwatched {}.", sender);
        }
    }
}
