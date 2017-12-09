package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.PoisonPill;
import akka.actor.dsl.Creators;
import de.hpi.akka_exercise.StudentList;
import java.io.Serializable;

import de.hpi.akka_exercise.messages.ShutdownMessage;
import de.hpi.akka_exercise.scheduling.SchedulingStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

abstract public class StudentAnalyzer extends AbstractLoggingActor {
    protected final ActorRef listener;
    protected final SchedulingStrategy schedulingStrategy;
    protected int nextQueryId = 0;

    public StudentAnalyzer(final ActorRef listener, SchedulingStrategy.Factory schedulingStrategyFactory, int numLocalWorkers) {
        this.listener = listener;
        this.schedulingStrategy = schedulingStrategyFactory.create(this.getSelf());
        for(int i = 0; i < numLocalWorkers; i++) {
            ActorRef worker = this.getContext().actorOf(Worker.props());
            this.schedulingStrategy.addWorker(worker);
            this.getContext().watch(worker);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class StudentsMessage implements Serializable {
        private StudentList students;
        private int numSplits;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class BeginWorkMessage implements Serializable {
        private String fileName;
        private ActorRef fileReader;
        private int numSplits;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemoteSystemMessage implements Serializable {
        @Getter private Address remoteAddress;
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
        log().info("Stopped {}.", this.getSelf());
    }

    protected abstract void handle(StudentsMessage message);

    protected void handle(BeginWorkMessage message) {
        message.fileReader.tell(new FileReader.ReadStudentsMessage(message.fileName, message.numSplits), this.getSelf());
    }

    protected boolean hasFinished() {
        return !this.schedulingStrategy.hasTasksInProgress();
    }

    protected void stopSelfAndListener() {
        this.listener.tell(new ShutdownMessage(), this.getSelf());
        this.getSelf().tell(PoisonPill.getInstance(), this.getSelf());
    }
}
