package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import de.hpi.akka_exercise.StudentList;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public class GeneAnalyser extends StudentAnalyzer {

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
