package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import de.hpi.akka_exercise.Student;
import de.hpi.akka_exercise.StudentList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.File;
import java.io.Serializable;

public class FileReader extends AbstractLoggingActor {
    private StudentList studentList;
    private ActorRef master;
    private boolean pwCracker;

    public FileReader(ActorRef master, boolean pwCracker) {
        this.master = master;
        this.pwCracker = pwCracker;
    }

    public static Props props() {
        return Props.create(FileReader.class);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadStudentsMessage implements Serializable {
        @Setter @Getter private String fileName;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ReadStudentsMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
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

    private void readStudents(String file) {
        try {
            File studentFile = new File(getClass().getResource(file).toURI());
            BufferedReader reader = new BufferedReader(new java.io.FileReader(studentFile));
            String line;
            StudentList students = new StudentList();
            while((line = reader.readLine()) != null) {
                Student student = new Student(line);
                students.addStudent(student);
            }
            this.studentList = students;
        } catch(Exception e) {
            this.log().error("Something went wrong {}.", e.getMessage());
        }
    }

    private void handle(ReadStudentsMessage message) {
        readStudents(message.getFileName());
        if(pwCracker) {
            this.master.tell(new PWCracker.StudentsMessage(studentList), this.getSelf());
        } else {
            this.master.tell(new GeneAnalyser.StudentsMessage(studentList), this.getSelf());
        }
    }
}
