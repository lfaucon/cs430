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
				return -1;
			} else {
				return 1;
			}
		}
	}
	
	public State getNewState(State previous, City moveTo, Task pickup, Task deliver) {
		State newState = new State(previous.currentCity);
		for(Task task : previous.restTasks) {
			newState.restTasks.add(task);
		}
		for(Task task : previous.currentTasks) {
			newState.currentTasks.add(task);
		}
		if(moveTo != null) {
			//System.out.println("moveto !");
			newState.currentCity = moveTo;
			newState.cost = previous.cost + previous.currentCity.distanceTo(moveTo) * this.costPerKm;
		}
		if(pickup != null) {
			System.out.println("pickup from "+pickup.pickupCity+" to "+pickup.deliveryCity);
			newState.restTasks.remove(pickup);
			newState.currentTasks.add(pickup);
			newState.weight = previous.weight + pickup.weight;
		}
		if(deliver != null) {
			System.out.println("deliver !");
			newState.currentTasks.remove(deliver);
		}
		return newState;
	}
	
	public class Action {
		City moveTo;
		Task pickup;
		Task deliver;
		
		public Action(City moveTo, Task pickup, Task deliver) {
			this.moveTo = moveTo;
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
		
		for(int step=0; step<100000; step++) { // no while() but long for() with break condition
			if(Q.isEmpty()) {
				break; // should never append
			}
			state = Q.poll();
			
			if(state.restTasks.isEmpty() && state.currentTasks.isEmpty()) {
				// first goal reached!
				goalReached = true;
				break; // should be "continue" because nothing guarantees that the first has the bes cost
				// should stop when a goal is reached and all other nodes in the queue has costs bigger or equals.
			}
			boolean delivers = false;
			
			for(Task task : state.currentTasks) {
				System.out.println("I got task to "+task.deliveryCity);
				if(task.deliveryCity==state.currentCity) {
					State newState = getNewState(state, null, null, task);
					Q.offer(newState);
					fatherState.put(newState, state);
					fatherAction.put(newState, new Action(null, null, task));
					delivers = true; // just explore this case, rest is idiot
					break;
				}
			}
			
			if(!delivers) {
				for(Task task : state.restTasks) {
					if(task.pickupCity==state.currentCity) {
						if(this.capacity >= state.weight+task.weight) {
							State newState = getNewState(state, null, task, null);
							Q.offer(newState);
							fatherState.put(newState, state);
							fatherAction.put(newState, new Action(null, task, null));
						}
					}
				}
				// must avoid city already visited if no new pickup before last visit
				// otherwise infinite loop
				for(City neighbor : state.currentCity.neighbors()) {
					State newState = getNewState(state, neighbor, null, null);
					Q.offer(newState);
					fatherState.put(newState, state);
					fatherAction.put(newState, new Action(neighbor, null, null));
				}
			}
		}
		if(!goalReached) {
			System.out.println("nique sa mere, on n'a pas trouve de goal");
			for(Task task : state.restTasks) {
				System.out.println("rest task in "+task.pickupCity);
			}
			for(Task task : state.currentTasks) {
				System.out.println("current tasks to "+task.pickupCity);
			}
			
		} else {
			System.out.println("goal trouve, planification...");
			Stack<Action> reversePlan = new Stack<Action>();
			State father = fatherState.get(state);
			while(father != null) {
				Action action = fatherAction.get(state);
				reversePlan.push(action);
				state = father;
			}
			while(!reversePlan.empty()) {
				Action action = reversePlan.pop();
				if(action.moveTo!=null) {
					plan.appendMove(action.moveTo);
				}
				if(action.pickup!=null) {
					plan.appendPickup(action.pickup);
				}
				if(action.deliver!=null) {
					plan.appendDelivery(action.deliver);
				}
			}
		}
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		// tasks = set of (pickupcity;deliverycity)
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
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
