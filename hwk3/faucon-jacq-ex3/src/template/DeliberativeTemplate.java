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
	String algorithmName;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		this.capacity = agent.vehicles().get(0).capacity();
		this.algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		// ...
	}

	public static State getNewState(State previous, Task pickup, Task deliver) {

		// Create new state
		State newState = new State(null);
		newState.cost += previous.cost;
		newState.g += previous.g;
		newState.weight += previous.weight;

		newState.restTasks.addAll(previous.restTasks);
		newState.currentTasks.addAll(previous.currentTasks);

		// if the action is a pickup
		if(pickup != null) {
			//System.out.println("pickup from "+pickup.pickupCity+" to "+pickup.deliveryCity);
			newState.cost += previous.currentCity.distanceTo(pickup.pickupCity);
			newState.g += previous.currentCity.distanceTo(pickup.pickupCity);
			newState.currentCity = pickup.pickupCity;
			newState.restTasks.remove(pickup);
			newState.currentTasks.add(pickup);
			newState.weight = previous.weight + pickup.weight;
		}

		// if the action is a deliver
		if(deliver != null) {
			//System.out.println("deliver task at "+deliver.deliveryCity);
			newState.cost += previous.currentCity.distanceTo(deliver.deliveryCity);
			newState.g += previous.currentCity.distanceTo(deliver.deliveryCity);
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

		System.out.println("###################################"+
				"\nWITH ALGORITHM "+algorithmName+
				"\nTOTAL DISTANCE: "+plan.totalDistance()+
				"\n###################################"
				);

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
