package de.hpi.akka_exercise.scheduling;

import akka.actor.ActorRef;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Router;
import de.hpi.akka_exercise.remote.actors.Worker;
import java.util.HashMap;
import java.util.Map;

public class RoundRobinSchedulingStrategy implements SchedulingStrategy {

	/**
	 * {@link SchedulingStrategy.Factory} implementation for the {@link RoundRobinSchedulingStrategy}.
	 */
	public static class Factory implements SchedulingStrategy.Factory {

		@Override
		public SchedulingStrategy create(ActorRef master) {
			return new RoundRobinSchedulingStrategy(master);
		}
	}

	// A round robin router for our workers
	private Router workerRouter = new Router(new RoundRobinRoutingLogic());

	// The number of workers currently available for scheduling
	private int numberOfWorkers = 0;

	// A map of pending responses for unfinished tasks
	private Map<Integer, Integer> taskId2numberPendingResponses = new HashMap<>();

	// A reference to the actor in whose name we send messages
	private final ActorRef master;

	public RoundRobinSchedulingStrategy(ActorRef master) {
		this.master = master;
	}

    @Override
    public void schedule(int taskId, Map<String, Integer> hashIndexMap, int startNumber, int endNumber) {
        this.workerRouter.route(new Worker.HashCrackMessage(taskId, hashIndexMap, startNumber, endNumber), this.master);
		// Store the task with numberOfWorkers pending responses
		this.taskId2numberPendingResponses.put(taskId, 1);
	}



    @Override
	public void finished(final int taskId, final ActorRef worker) {

		// Decrement the number of pending responses for this task
		final int newPendingResponses = this.taskId2numberPendingResponses.get(taskId) - 1;

		if (newPendingResponses == 0) {
			// Task is completed
			this.taskId2numberPendingResponses.remove(taskId);
		} else {
			// Task is still pending
			this.taskId2numberPendingResponses.put(taskId, newPendingResponses);
		}
	}

	@Override
	public boolean hasTasksInProgress() {
		return !this.taskId2numberPendingResponses.isEmpty();
	}

	@Override
	public void addWorker(final ActorRef worker) {

		// Increment the worker count
		this.numberOfWorkers++;

		// Add the worker to the router
		this.workerRouter = this.workerRouter.addRoutee(worker);
	}

	@Override
	public void removeWorker(final ActorRef worker) {

		// Decrement the worker count
		this.numberOfWorkers--;

		// Remove the worker from the router
		this.workerRouter = this.workerRouter.removeRoutee(worker);
	}

	@Override
	public int countWorkers() {
		return this.numberOfWorkers;
	}
}
