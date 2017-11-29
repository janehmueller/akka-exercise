package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import de.hpi.akka_exercise.StudentList;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// TODO: shared superclass
public class GeneAnalyser extends AbstractLoggingActor {

    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentsMessage implements Serializable {
        private StudentList students;
    }

    public Props props() {
        return Props.create(GeneAnalyser.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(GeneAnalyser.StudentsMessage.class, this::handle)
            .matchAny(object -> this.log().error("Could not understand received message."))
            .build();
    }

    private void handle(StudentsMessage message) {
        return;
    }
}
