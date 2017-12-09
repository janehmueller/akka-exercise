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

@NoArgsConstructor
public class FileReader extends AbstractLoggingActor {
    public static final String DEFAULT_NAME = "filereader";

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

    private StudentList readStudents(String file) {
        StudentList studentList = new StudentList();
        try {
            File studentFile = new File(getClass().getResource(file).toURI());
            BufferedReader reader = new BufferedReader(new java.io.FileReader(studentFile));
            String line;
            StudentList students = new StudentList();
            while((line = reader.readLine()) != null) {
                Student student = new Student(line);
                students.addStudent(student);
            }
            studentList = students;
        } catch(Exception e) {
            this.log().error("Something went wrong {}.", e.getMessage());
        }
        return studentList;
    }

    private void handle(ReadStudentsMessage message) {
        StudentList students = readStudents(message.getFileName());
        this.getSender().tell(new StudentAnalyzer.StudentsMessage(students), this.getSelf());
    }
}
