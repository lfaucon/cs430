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

	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;
	int costPerKm;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		this.capacity = agent.vehicles().get(0).capacity();
		this.costPerKm = agent.vehicles().get(0).costPerKm();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	public class State implements Comparable<Object>{
		public City currentCity;
		public HashSet<Task> restTasks; //all remaining tasks
		public HashSet<Task> currentTasks; // all picked tasks
		public Double cost;
		public Double weight;
		
		public State(City currentCity) {
			this.currentCity = currentCity;
			this.restTasks = new HashSet<Task>();
			this.currentTasks = new HashSet<Task>();
			this.cost = 0.;
			this.weight = 0.;
		}
		
		@Override
		public int compareTo(Object otherState) {
			if(this.cost>((State)otherState).cost) {
				return 1;
			} else {
				return -1;
			}
		}
	}
	
	public State getNewState(State previous, Task pickup, Task deliver) {
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
			System.out.println("pickup from "+pickup.pickupCity+" to "+pickup.deliveryCity);
			newState.cost += previous.currentCity.distanceTo(pickup.pickupCity) * this.costPerKm;
			newState.currentCity = pickup.pickupCity;
			newState.restTasks.remove(pickup);
			newState.currentTasks.add(pickup);
			newState.weight = previous.weight + pickup.weight;
		}
		if(deliver != null) {
			System.out.println("deliver task at "+deliver.deliveryCity);
			newState.cost += previous.currentCity.distanceTo(deliver.deliveryCity) * this.costPerKm;
			newState.currentCity = deliver.deliveryCity;
			newState.currentTasks.remove(deliver);
		}
		return newState;
	}
	
	public class Action {
		Task pickup;
		Task deliver;
		
		public Action(Task pickup, Task deliver) {
			this.pickup = pickup;
			this.deliver = deliver;
		}
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = bfsPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {
		City currentCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currentCity);
		State state = new State(currentCity);
		for(Task task : tasks) {
			state.restTasks.add(task);
		}
		PriorityQueue<State> Q = new PriorityQueue<State>();
		Q.offer(state);
		HashMap<State,State> fatherState = new HashMap<State,State>();
		fatherState.put(state, null);
		HashMap<State,Action> fatherAction = new HashMap<State,Action>();
		fatherAction.put(state, null);
		boolean goalReached = false;
		
		for(int step=0; step<1000000; step++) { // no while() but long for() with break condition
			if(Q.isEmpty()) {
				System.out.println("empty tree after "+step+" iterations");
				break;
			}
			
			state = Q.poll();
			if(state.restTasks.isEmpty() && state.currentTasks.isEmpty()) {
				// first goal reached necessarily the one with optimal cost
				System.out.println("goal found after "+step+" iterations");
				goalReached = true;
				break;
			}
			
			boolean delivers_closer = false;
			for(Task pickup : state.restTasks) {// probably easy to optimize this double loop:
				for(City city : state.currentCity.pathTo(pickup.pickupCity)) {
					for(Task deliverCloser : state.currentTasks) {
						if(city==deliverCloser.deliveryCity) {
							delivers_closer = true; // passing a city where we can deliver is idiot
							break;
						}
					}
				}
				if(!delivers_closer) {
					if(this.capacity>state.weight+pickup.weight) {
						State newState = getNewState(state, pickup, null);
						Q.offer(newState);
						fatherState.put(newState, state);
						fatherAction.put(newState, new Action(pickup, null));
					}
				}
			}
			
			delivers_closer = false; 
			for(Task deliver : state.currentTasks) {// probably easy to optimize this double loop:
				for(City city : state.currentCity.pathTo(deliver.deliveryCity)) {
					for(Task deliverCloser : state.currentTasks) {
						if(city==deliverCloser.deliveryCity && city!=deliver.deliveryCity) {
							delivers_closer = true; // passing a city where we can deliver is idiot
							break;
						}
					}
				}
				if(!delivers_closer) {
					State newState = getNewState(state, null, deliver);
					Q.offer(newState);
					fatherState.put(newState, state);
					fatherAction.put(newState, new Action(null, deliver));
				}
			}
		}
		if(!goalReached) {
			System.out.println("nique sa mere, on n'a pas trouve de goal");
		} else {
			System.out.println("goal found with cost "+state.cost+" planification...");
			Stack<Action> reversePlan = new Stack<Action>();
			int i = 0;
			while(state != null) {
				if(fatherState.get(state)!=null) {
					System.out.println("state city "+state.currentCity+" father state "+fatherState.get(state).currentCity);
					Action action = fatherAction.get(state);
					reversePlan.push(action);
				}
				state = fatherState.get(state);
			}
			while(!reversePlan.empty()) {
				Action action = reversePlan.pop();
				if(action.pickup!=null) {
					System.out.println("action pickup "+action.pickup.pickupCity);
					for (City city : currentCity.pathTo(action.pickup.pickupCity))
						plan.appendMove(city);
					plan.appendPickup(action.pickup);
					currentCity = action.pickup.pickupCity;
				}
				if(action.deliver!=null) {
					System.out.println("action deliver "+action.deliver.deliveryCity);
					for (City city : currentCity.pathTo(action.deliver.deliveryCity))
						plan.appendMove(city);
					plan.appendDelivery(action.deliver);
					currentCity = action.deliver.deliveryCity;
				}
			}
		}
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		// tasks = set of (pickupcity;deliverycity)
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		Double cost = 0.;

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);
			cost += current.distanceTo(task.pickupCity) * this.costPerKm;

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);
			cost += task.pickupCity.distanceTo(task.deliveryCity) * this.costPerKm;

			// set current city
			current = task.deliveryCity;
		}
		System.out.println("found plan with cost "+cost);
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
