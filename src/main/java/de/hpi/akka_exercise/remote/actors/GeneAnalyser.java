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
    public void preStart() throws Exception {
        super.preStart();
        Reaper.watchWithDefaultReaper(this);
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        // TODO stop listener
        log().info("Stopped {}.", this.getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(GeneAnalyser.StudentsMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
    }

    private void handle(StudentsMessage message) {
        return;
    }
}
