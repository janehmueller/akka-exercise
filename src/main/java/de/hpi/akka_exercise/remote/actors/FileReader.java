package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import com.typesafe.config.ConfigException;
import de.hpi.akka_exercise.Student;
import de.hpi.akka_exercise.StudentList;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@NoArgsConstructor
public class FileReader extends AbstractLoggingActor {
    public static final String DEFAULT_NAME = "filereader";

    public static Props props() {
        return Props.create(FileReader.class);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadStudentsMessage implements Serializable {
        private String fileName;
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
        this.log().info("Stopped {}.", this.getSelf());
    }

    private StudentList readStudents(String file) {
        StudentList studentList = new StudentList();
        try {
            File studentFile = new File(getClass().getResource("/" + file).toURI());
            BufferedReader reader = new BufferedReader(new java.io.FileReader(studentFile));
            String line;
            StudentList students = new StudentList();
            while((line = reader.readLine()) != null) {
                if(!line.isEmpty()) {
                    Student student = new Student(line);
                    students.addStudent(student);
                }
            }
            studentList = students;
        } catch(URISyntaxException | FileNotFoundException e) {
            this.log().error("Error opening file {}: {}", file, e.getMessage());
        } catch(IOException e) {
            this.log().error("Error reading contents of file {}:", file, e.getMessage());
        }
        return studentList;
    }

    private void handle(ReadStudentsMessage message) {
        StudentList students = readStudents(message.fileName);
        this.getSender().tell(new Master.StudentsMessage(students), this.getSelf());
    }
}
