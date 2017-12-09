package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import de.hpi.akka_exercise.StudentList;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

public class PWCracker extends AbstractLoggingActor {
    private StudentList studentList;

    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentsMessage implements Serializable {
        private StudentList students;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordMessage implements Serializable {
        private Map<Integer, String> passwordMap;
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
            .match(PasswordMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
    }


    private void handle(StudentsMessage message) {
        return;
    }

    private void handle(PasswordMessage message) {
        for(Map.Entry<Integer, String> entry : message.passwordMap.entrySet()) {
            int index = entry.getKey();
            String password = entry.getValue();
            studentList.updateStudentPassword(index, password);
        }
    }
}
