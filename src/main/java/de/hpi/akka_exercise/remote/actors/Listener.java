package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import de.hpi.akka_exercise.StudentList;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

public class Listener extends AbstractLoggingActor {
    private StudentList studentList;

    public static Props props() { return Props.create(Listener.class); }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogPasswordMessage implements Serializable {
        private Map<Integer, String> indexPasswordMap;
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        Reaper.watchWithDefaultReaper(this);
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        log().info("Stopped {}.", this.getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(LogPasswordMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
    }

    private void handle(LogPasswordMessage message) {
        for(Map.Entry<Integer, String> entry : message.indexPasswordMap.entrySet()) {
            int index = entry.getKey();
            String password = entry.getValue();
            studentList.updateStudentPassword(index, password);
            this.log().info("Cracked password for student {} ({}): {}", index, studentList.getStudent(index).getName() , password);
        }
    }
}
