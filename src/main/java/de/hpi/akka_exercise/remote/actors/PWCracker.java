package de.hpi.akka_exercise.remote.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import de.hpi.akka_exercise.scheduling.SchedulingStrategy;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

public class PWCracker extends StudentAnalyzer {
    public static final String DEFAULT_NAME = "pwcracker";

    public PWCracker(final ActorRef listener, SchedulingStrategy.Factory schedulingStrategyFactory, int numLocalWorkers) {
        super(listener, schedulingStrategyFactory, numLocalWorkers);
    }

    public static Props props(final ActorRef listener, SchedulingStrategy.Factory schedulingStrategyFactory, final int numLocalWorkers) {
        return Props.create(PWCracker.class, () -> new PWCracker(listener, schedulingStrategyFactory, numLocalWorkers));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordMessage implements Serializable {
        private int requestId;
        private Map<Integer, String> indexPasswordMap;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(StudentsMessage.class, this::handle)
            .match(PasswordMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
        // TODO final message to file write to write results
        // TODO message to listener
    }

    protected void handle(StudentsMessage message) {
        this.listener.tell(new Listener.StudentMessage(message.getStudents()), this.getSelf());
        // TODO schedule
    }

    private void handle(PasswordMessage message) {
        this.listener.tell(new Listener.LogPasswordMessage(message.indexPasswordMap), this.getSelf());
        this.schedulingStrategy.finished(message.requestId, this.getSender());
        if(this.hasFinished()) {
            this.stopSelfAndListener();
        }
    }
}
