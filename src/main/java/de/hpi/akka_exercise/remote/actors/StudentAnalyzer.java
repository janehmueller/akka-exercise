package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import de.hpi.akka_exercise.StudentList;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

abstract public class StudentAnalyzer extends AbstractLoggingActor {

    protected StudentList studentList;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class StudentsMessage implements Serializable {
        private StudentList students;
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

    protected abstract void handle(StudentsMessage message);
}
