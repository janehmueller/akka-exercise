package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.PoisonPill;
import akka.actor.Props;
import de.hpi.akka_exercise.Student;
import de.hpi.akka_exercise.StudentList;
import de.hpi.akka_exercise.messages.ShutdownMessage;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

public class Listener extends AbstractLoggingActor {

    public static final String DEFAULT_NAME = "listener";

    private StudentList studentList;

    public static Props props() { return Props.create(Listener.class); }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentMessage implements Serializable {
        private StudentList students;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogPasswordMessage implements Serializable {
        private Map<Integer, String> indexPasswordMap;
    }

    @NoArgsConstructor
    public static class LogMessage implements Serializable {}

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
            .match(StudentMessage.class, this::handle)
            .match(LogPasswordMessage.class, this::handle)
            .match(LogMessage.class, this::handle)
            .match(ShutdownMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
    }

    private void handle(StudentMessage message) {
        this.studentList = message.students;
    }

    private void handle(LogPasswordMessage message) {
        for(Map.Entry<Integer, String> entry : message.indexPasswordMap.entrySet()) {
            int index = entry.getKey();
            String password = entry.getValue();
            studentList.updateStudentPassword(index, password);
            this.log().info("Cracked password for student {} ({}): {}", index, studentList.getStudent(index).getName() , password);
        }
    }

    private void handle(LogMessage message) {
        if (studentList == null) {
            this.log().info("No passwords have been cracked yet.");
            return;
        }
        this.log().info("Cracked passwords for the following students:");
        for(Student student : studentList.getStudents()) {
            if(student.isCracked()) {
                String name = String.format("Student %2d %23s", student.getIndex(), "(" + student.getName() + ")");
                this.log().info("{} with password {} and hash {}", name, student.getPassword(), student.getPasswordHash());
            }
        }
    }

    private void handle(ShutdownMessage message) {
        // TODO write students to disk
        this.getSelf().tell(PoisonPill.getInstance(), this.getSelf());
    }
}
