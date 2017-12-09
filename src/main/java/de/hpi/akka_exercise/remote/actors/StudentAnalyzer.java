package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import de.hpi.akka_exercise.StudentList;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

abstract public class StudentAnalyzer extends AbstractLoggingActor{

    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentsMessage implements Serializable {
        private StudentList students;
    }

    public Props props() {
        return Props.create(PWCracker.class);
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
            .match(PWCracker.StudentsMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
    }

    private void handle(PWCracker.StudentsMessage message) {
        throw new NotImplementedException();
    }
}
