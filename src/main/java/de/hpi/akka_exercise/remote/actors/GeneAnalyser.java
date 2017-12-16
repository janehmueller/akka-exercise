package de.hpi.akka_exercise.remote.actors;

import akka.actor.ActorRef;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.remote.RemoteScope;
import de.hpi.akka_exercise.scheduling.SchedulingStrategy;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public class GeneAnalyser extends StudentAnalyzer {
    public static final String DEFAULT_NAME = "geneanalyzer";

    public GeneAnalyser(final ActorRef listener, SchedulingStrategy.Factory schedulingStrategyFactory, int numLocalWorkers) {
        super(listener, schedulingStrategyFactory, numLocalWorkers);
    }

    public static Props props(final ActorRef listener, SchedulingStrategy.Factory schedulingStrategyFactory, final int numLocalWorkers) {
        return Props.create(GeneAnalyser.class, () -> new GeneAnalyser(listener, schedulingStrategyFactory, numLocalWorkers));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneMatchMessage implements Serializable {
        private int requestId;
        private int studentIdX;
        private int studentIdY;
        private String mostCommonSubstring;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(StudentsMessage.class, this::handle)
            .match(BeginWorkMessage.class, this::handle)
            .match(GeneMatchMessage.class, this::handle)
            .match(RemoteSystemMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
    }

    protected void handle(StudentsMessage message) {
        this.listener.tell(new Listener.StudentMessage(message.getStudents()), this.getSelf());
        int numStudents = message.getStudents().numStudents();
        for (int i = 0; i < numStudents; i++) {
            for (int j = i + 1; j < numStudents; j++) {
                this.schedulingStrategy.schedule(nextQueryId, message.getStudents().getStudent(i), message.getStudents().getStudent(j));
                this.nextQueryId++;
            }
        }
    }

    private void handle(GeneMatchMessage message) {
        this.listener.tell(new Listener.LogGeneMatchMessage(message.studentIdX, message.studentIdY, message.mostCommonSubstring), this.getSelf());
        this.schedulingStrategy.finished(message.requestId, this.getSender());
        if(this.hasFinished()) {
            this.stopSelfAndListener();
        }
    }

    private void handle(RemoteSystemMessage message) {
        ActorRef worker = this.getContext().actorOf(Worker.props().withDeploy(new Deploy(new RemoteScope(message.getRemoteAddress()))));
        this.schedulingStrategy.addWorker(worker);
        this.getContext().watch(worker);
        this.log().info("New worker: {}.", worker);
    }
}
