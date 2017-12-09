package de.hpi.akka_exercise.remote.actors;

import akka.actor.Props;

public class GeneAnalyser extends StudentAnalyzer {
    public static final String DEFAULT_NAME = "geneanalyzer";

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
