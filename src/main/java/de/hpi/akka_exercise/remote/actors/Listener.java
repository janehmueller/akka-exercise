package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.PoisonPill;
import akka.actor.Props;
import de.hpi.akka_exercise.Student;
import de.hpi.akka_exercise.StudentList;
import de.hpi.akka_exercise.messages.ShutdownMessage;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.PrintWriter;
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
    @AllArgsConstructor
    public static class LogGeneMatchMessage implements Serializable {
        private int studentIdX;
        private int studentIdY;
        private String mostCommonSubstring;
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

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(StudentMessage.class, this::handle)
            .match(LogPasswordMessage.class, this::handle)
            .match(LogGeneMatchMessage.class, this::handle)
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
            this.log().info("Cracked password for student {}: {}", studentList.getStudent(index).formattedName(), password);
        }
    }

    private void handle(LogGeneMatchMessage message) {
        Student x = studentList.getStudent(message.studentIdX);
        Student y = studentList.getStudent(message.studentIdY);
        this.log().info("Finished comparing genome sequence for students {}.", String.format("%2d and %2d", x.getIndex(), y.getIndex()));
        x.updateGenomeNeighbor(message.studentIdY, message.mostCommonSubstring);
        y.updateGenomeNeighbor(message.studentIdX, message.mostCommonSubstring);
        if(x.isGenomeNeighborFound(studentList.numStudents())) {
            this.log().info("Genome neighbor for student {} found: student {} with genome substring {}", x.formattedName(), String.format("%2d", x.getClosestGenomeSequenceNeighbor()), x.getClosestGenomeSequence());
        }
        if(y.isGenomeNeighborFound(studentList.numStudents())) {
            this.log().info("Genome neighbor for student {} found: student {} with genome substring {}", y.formattedName(), String.format("%2d", y.getClosestGenomeSequenceNeighbor()), y.getClosestGenomeSequence());
        }
    }

    private void handle(ShutdownMessage message) {
        try {
            String fileName = "updatedStudents.csv";
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            for(String line : studentList.toCSV()) {
                log().info("Writing: {}", line);
                writer.write(line);
                writer.write("\n");
            }
            writer.close();
            this.log().info("Wrote results to file \"{}\".", fileName);
        } catch (IOException e) {
            this.log().error("Error while writing result file: {}.", e.getMessage());
        }
        this.getSelf().tell(PoisonPill.getInstance(), this.getSelf());
    }
}
