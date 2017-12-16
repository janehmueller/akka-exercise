package de.hpi.akka_exercise.remote.actors;

import akka.actor.ActorRef;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.remote.RemoteScope;
import de.hpi.akka_exercise.scheduling.SchedulingStrategy;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

public class PWCracker extends StudentAnalyzer {
    public static final String DEFAULT_NAME = "pwcracker";
    public static final int numPasswords = 10000000;

    public PWCracker(final ActorRef listener, SchedulingStrategy.Factory schedulingStrategyFactory, int numLocalWorkers) {
        super(listener, schedulingStrategyFactory, numLocalWorkers);
    }

    public static Props props(final ActorRef listener, SchedulingStrategy.Factory schedulingStrategyFactory, final int numLocalWorkers) {
        return Props.create(PWCracker.class, () -> new PWCracker(listener, schedulingStrategyFactory, numLocalWorkers));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordMessage implements Serializable {
        private int requestId;
        private Map<Integer, String> indexPasswordMap;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(StudentsMessage.class, this::handle)
            .match(PasswordMessage.class, this::handle)
            .match(BeginWorkMessage.class, this::handle)
            .match(RemoteSystemMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
        // TODO final message to file write to write results
        // TODO message to listener
    }

    protected void handle(StudentsMessage message) {
        this.listener.tell(new Listener.StudentMessage(message.getStudents()), this.getSelf());
        int intervalSize =  Math.max(numPasswords / message.getNumSplits(), 1);
        // TODO every range is scheduled twice
        for(int i = 0; i < numPasswords; i += intervalSize) {
            int rangeEnd = Math.min(i + intervalSize, numPasswords);
            this.schedulingStrategy.schedule(nextQueryId, message.getStudents().createHashIndexMap(), i, rangeEnd);
            this.nextQueryId++;
        }
    }

    private void handle(PasswordMessage message) {
        this.listener.tell(new Listener.LogPasswordMessage(message.indexPasswordMap), this.getSelf());
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
