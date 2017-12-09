package de.hpi.akka_exercise.remote.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import de.hpi.akka_exercise.scheduling.SchedulingStrategy;

public class GeneAnalyser extends StudentAnalyzer {
    public static final String DEFAULT_NAME = "geneanalyzer";

    public GeneAnalyser(final ActorRef listener, SchedulingStrategy.Factory schedulingStrategyFactory, int numLocalWorkers) {
        super(listener, schedulingStrategyFactory, numLocalWorkers);
    }

    public static Props props() { return Props.create(GeneAnalyser.class); }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(StudentsMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
    }

    protected void handle(StudentsMessage message) {
        this.studentList = message.getStudents();
        // TODO schedule
    }
}
