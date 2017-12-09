package de.hpi.akka_exercise.remote.actors;

import akka.actor.Props;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

public class PWCracker extends StudentAnalyzer {
    public static final String DEFAULT_NAME = "pwcracker";

    public static Props props() { return Props.create(PWCracker.class); }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordMessage implements Serializable {
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
        this.studentList = message.getStudents();
        // TODO schedule
    }

    private void handle(PasswordMessage message) {
        for(Map.Entry<Integer, String> entry : message.indexPasswordMap.entrySet()) {
            int index = entry.getKey();
            String password = entry.getValue();
            studentList.updateStudentPassword(index, password);
        }
    }
}
