package template;

/* import table */
import java.util.*;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR, QFS, NAIVE }

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		this.capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		// ...
	}

	public static State getNewState(State previous, Task pickup, Task deliver) {
		State newState = new State(previous.currentCity);
		newState.cost += previous.cost;
		newState.weight += previous.weight;
		for(Task task : previous.restTasks) {
			newState.restTasks.add(task);
		}
		for(Task task : previous.currentTasks) {
			newState.currentTasks.add(task);
		}
		if(pickup != null) {
			//System.out.println("pickup from "+pickup.pickupCity+" to "+pickup.deliveryCity);
			newState.cost += previous.currentCity.distanceTo(pickup.pickupCity);
			newState.currentCity = pickup.pickupCity;
			newState.restTasks.remove(pickup);
			newState.currentTasks.add(pickup);
			newState.weight = previous.weight + pickup.weight;
		}
		if(deliver != null) {
			//System.out.println("deliver task at "+deliver.deliveryCity);
			newState.cost += previous.currentCity.distanceTo(deliver.deliveryCity);
			newState.currentCity = deliver.deliveryCity;
			newState.currentTasks.remove(deliver);
		}
		return newState;
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			plan = ASTAR.getPlan(vehicle, tasks);
			break;
		case BFS:
			plan = BFS.getPlan(vehicle, tasks);
			break;
		case QFS:
			plan = ASTAR.getPlan(vehicle, tasks);
			break;
		case NAIVE:
			plan = NAIVE.getPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {

		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
