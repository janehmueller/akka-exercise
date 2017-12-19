package de.hpi.akka_exercise.remote.actors;

import akka.actor.*;
import akka.remote.RemoteScope;
import de.hpi.akka_exercise.StudentList;
import de.hpi.akka_exercise.messages.ShutdownMessage;
import de.hpi.akka_exercise.scheduling.SchedulingStrategy;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

public class Master extends AbstractLoggingActor {
    public static final String DEFAULT_NAME = "master";
    private static final int numPasswords = 10000000;
    private final ActorRef listener;
    private final SchedulingStrategy schedulingStrategy;
    private int nextQueryId = 0;
    private int numPasswordSplits;
    private boolean crackPasswords;
    private boolean compareGenomes;

    public Master(final ActorRef listener, SchedulingStrategy.Factory schedulingStrategyFactory, int numLocalWorkers) {
        this.listener = listener;
        this.schedulingStrategy = schedulingStrategyFactory.create(this.getSelf());
        for(int i = 0; i < numLocalWorkers; i++) {
            ActorRef worker = this.getContext().actorOf(Worker.props());
            this.schedulingStrategy.addWorker(worker);
            this.getContext().watch(worker);
        }
    }

    public static Props props(final ActorRef listener, SchedulingStrategy.Factory schedulingStrategyFactory, final int numLocalWorkers) {
        return Props.create(Master.class, () -> new Master(listener, schedulingStrategyFactory, numLocalWorkers));
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        Reaper.watchWithDefaultReaper(this);
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        this.listener.tell(PoisonPill.getInstance(), this.getSelf());
        this.log().info("Stopped {}.", this.getSelf());
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class BeginWorkMessage implements Serializable {
        private String fileName;
        private ActorRef fileReader;
        private int numSplits;
        private boolean crackPasswords;
        private boolean compareGenomes;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentsMessage implements Serializable {
        private StudentList students;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordMessage implements Serializable {
        private int requestId;
        private Map<Integer, String> indexPasswordMap;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenomeMatchMessage implements Serializable {
        private int requestId;
        private int studentIdX;
        private int studentIdY;
        private String mostCommonSubstring;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemoteSystemMessage implements Serializable {
        private Address remoteAddress;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(BeginWorkMessage.class, this::handle)
            .match(StudentsMessage.class, this::handle)
            .match(PasswordMessage.class, this::handle)
            .match(GenomeMatchMessage.class, this::handle)
            .match(RemoteSystemMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
    }

    private void handle(BeginWorkMessage message) {
        numPasswordSplits = message.numSplits;
        crackPasswords = message.crackPasswords;
        compareGenomes = message.compareGenomes;
        message.fileReader.tell(new FileReader.ReadStudentsMessage(message.fileName), this.getSelf());
    }

    private void handle(StudentsMessage message) {
        this.listener.tell(new Listener.StudentMessage(message.students), this.getSelf());
        if(crackPasswords) {
            schedulePasswordTasks(message);
        }
        if(compareGenomes) {
            scheduleGenomeTasks(message);
        }
    }

    private void schedulePasswordTasks(StudentsMessage message) {
        int intervalSize =  Math.max(numPasswords / numPasswordSplits, 1);
        // TODO every range is scheduled twice
        for(int i = 0; i < numPasswords; i += intervalSize) {
            int rangeEnd = Math.min(i + intervalSize, numPasswords);
            this.schedulingStrategy.schedule(nextQueryId, message.students.createHashIndexMap(), i, rangeEnd);
            this.nextQueryId++;
        }
    }

    private void scheduleGenomeTasks(StudentsMessage message) {
        int numStudents = message.students.numStudents();
        for (int i = 0; i < numStudents; i++) {
            for (int j = i + 1; j < numStudents; j++) {
                this.schedulingStrategy.schedule(nextQueryId, message.students.getStudent(i), message.students.getStudent(j));
                this.nextQueryId++;
            }
        }
    }

    private void handle(PasswordMessage message) {
        this.listener.tell(new Listener.LogPasswordMessage(message.indexPasswordMap), this.getSelf());
        this.schedulingStrategy.finished(message.requestId, this.getSender());
        if(this.hasFinished()) {
            this.stopSelfAndListener();
        }
    }

    private void handle(GenomeMatchMessage message) {
        this.listener.tell(new Listener.LogGeneMatchMessage(message.studentIdX, message.studentIdY, message.mostCommonSubstring), this.getSelf());
        this.schedulingStrategy.finished(message.requestId, this.getSender());
        if(this.hasFinished()) {
            this.stopSelfAndListener();
        }
    }

    private void handle(RemoteSystemMessage message) {
        ActorRef worker = this.getContext().actorOf(Worker.props().withDeploy(new Deploy(new RemoteScope(message.remoteAddress))));
        this.schedulingStrategy.addWorker(worker);
        this.getContext().watch(worker);
        this.log().info("New worker: {}.", worker);
    }

    private boolean hasFinished() {
        return !this.schedulingStrategy.hasTasksInProgress();
    }

    private void stopSelfAndListener() {
        this.listener.tell(new ShutdownMessage(), this.getSelf());
        this.getSelf().tell(PoisonPill.getInstance(), this.getSelf());
    }
}
