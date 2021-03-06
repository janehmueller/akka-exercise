package de.hpi.akka_exercise.remote.actors;

import akka.actor.*;
import akka.remote.DisassociatedEvent;
import de.hpi.akka_exercise.messages.ShutdownMessage;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * The slave actor tries to subscribe its actor system to a shepherd actor in a master actor system.
 */
public class Slave extends AbstractLoggingActor {
    public static final String DEFAULT_NAME = "slave";

    /**
     * Create the {@link Props} necessary to instantiate new {@link Slave} actors.
     *
     * @return the {@link Props}
     */
    public static Props props() {
        return Props.create(Slave.class);
    }

    /**
     * Asks the {@link Slave} to subscribe to a (remote) {@link Shepherd} actor with a given address.
     */
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressMessage implements Serializable {
        private Address address;
    }

    /**
     * Asks the {@link Slave} to acknowledge a successful connection request with a (remote) {@link Shepherd} actor.
     */
    @NoArgsConstructor
    public static class AcknowledgementMessage implements Serializable {}

    // A scheduling item to keep on trying to reconnect as regularly
    private Cancellable connectSchedule;

    @Override
    public void preStart() throws Exception {
        super.preStart();

        // Register at this actor system's reaper
        Reaper.watchWithDefaultReaper(this);

        // Listen for disassociation with the master
        this.getContext().getSystem().eventStream().subscribe(this.getSelf(), DisassociatedEvent.class);
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();

        // Log the stop event
        this.log().info("Stopped {}.", this.getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(AddressMessage.class, this::handle)
            .match(AcknowledgementMessage.class, this::handle)
            .match(ShutdownMessage.class, this::handle)
            .match(DisassociatedEvent.class, this::handle)
            .matchAny(object -> this.log().error("Received unknown message: \"{}\" ({})", object, object.getClass()))
            .build();
    }

    private void handle(ShutdownMessage message) {

        // Log remote shutdown message
        this.log().info("Was asked to stop.");

        // Stop self by sending a poison pill
        this.getSelf().tell(PoisonPill.getInstance(), this.getSelf());
    }

    private void handle(AddressMessage message) {

        // Cancel any running connect schedule, because got a new address
        if (this.connectSchedule != null) {
            this.connectSchedule.cancel();
            this.connectSchedule = null;
        }

        // Find the shepherd actor in the remote actor system
        final ActorSelection selection = this.getContext().getSystem().actorSelection(String.format("%s/user/%s", message.address, Shepherd.DEFAULT_NAME));

        // Register the local actor system by periodically sending subscription messages (until an acknowledgement was received)
        final Scheduler scheduler = this.getContext().getSystem().scheduler();
        final ExecutionContextExecutor dispatcher = this.getContext().getSystem().dispatcher();
        this.connectSchedule = scheduler.schedule(
            Duration.Zero(),
            Duration.create(5, TimeUnit.SECONDS),
            () -> selection.tell(new Shepherd.SubscriptionMessage(), this.getSelf()),
            dispatcher
        );
    }

    private void handle(AcknowledgementMessage message) {

        // Cancel any running connect schedule, because we are now connected
        if (this.connectSchedule != null) {
            this.connectSchedule.cancel();
            this.connectSchedule = null;
        }

        // Log the connection success
        this.log().info("Subscription successfully acknowledged by {}.", this.getSender());
    }

    private void handle(DisassociatedEvent event) {

        // Disassociations are a problem only once we have a running connection, i.e., no connection schedule is active; they do not concern this actor otherwise.
        if (this.connectSchedule == null) {
            this.log().error("Disassociated from master. Stopping...");
            this.getContext().stop(this.getSelf());
        }
    }
}
