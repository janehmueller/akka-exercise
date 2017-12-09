package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import de.hpi.akka_exercise.StudentList;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

public class PWCracker extends StudentAnalyzer {

    private StudentList studentList;

    public Props props() {
        return Props.create(PWCracker.class);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordMessage implements Serializable {
        private Map<Integer, String> passwordMap;

    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(StudentAnalyzer.StudentsMessage.class, this::handle)
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
