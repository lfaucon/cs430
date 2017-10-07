package template;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;

	private HashMap<City, HashMap<City, State>> states;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		int costPerKm = agent.vehicles().get(0).costPerKm();

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		states = new HashMap<City, HashMap<City, State>>();

		for (City cityFrom: topology.cities()){
			// add empty hashmap for states starting from cityFrom
			HashMap<City, State> hm = new HashMap<City, State>();
			states.put(cityFrom, hm);
			// add state from cityFrom with no task
			hm.put(null, new State(cityFrom, null, td, costPerKm));
			for (City cityTo: topology.cities()){
				// add state from cityFrom with a task for cityTo
				hm.put(cityTo, new State(cityFrom, cityTo, td, costPerKm));
			}
		}

		for(int step=0; step<10000; step++){
			for(HashMap<City, State> hm: states.values()){
				for(State state: hm.values()){
					state.updateValues(discount, topology.cities(), states, td);
				}
			}
			for(HashMap<City, State> hm: states.values()){
				for(State state: hm.values()){
					state.updateBestAction();
				}
			}
		}
	}

	// Define the class state
	public class State {

		public City cityFrom;
		public City cityTo;

		// The computation of the expected value
		private HashMap<City,Double> values;
		// the immediate reward for each action
		private HashMap<City,Double> rewards;

		public double bestValue = 0.;
		public City bestAction = null;

		public State(City from, City to, TaskDistribution td, int costPerKm){
			cityFrom = from;
			cityTo = to;
			values = new HashMap<City,Double>();
			rewards = new HashMap<City,Double>();
			// moving actions
			for(City neighbor: cityFrom.neighbors()){
				values.put(neighbor, 0.);
				rewards.put(neighbor, - from.distanceTo(neighbor) * costPerKm);
			}
			// delivering action
			if(cityTo != null){
				values.put(to, 0.);
				rewards.put(to, td.reward(from, to) - from.distanceTo(to) * costPerKm);
			}
		}

		public void updateValues(double discount, List<City> cities, HashMap<City, HashMap<City, State>> states, TaskDistribution td){
			// For each action (corresponding to a new city)
			for(City newCity: values.keySet()){
				// initialize the value with the immediate reward
				double newValue = rewards.get(newCity);

				double noTaskProbability = 1.;
				for(City cityTo: cities){
					newValue += discount * td.probability(newCity,cityTo) * states.get(newCity).get(cityTo).bestValue;
					noTaskProbability -= td.probability(newCity,cityTo);
				}
				newValue += discount * noTaskProbability * states.get(newCity).get(null).bestValue;

				// updates value table
				values.put(newCity, newValue);
			}
		}

		public void updateBestAction() {
			double maxValue = 0.;
			City maxAction = null;
			for(City a: values.keySet()){
				double v = values.get(a);
				if(v > maxValue){
					maxValue = v;
					maxAction = a;
				}
			}
			bestValue = maxValue;
			bestAction = maxAction;
		}
	}



	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		// Current City
		City currentCity = vehicle.getCurrentCity();
		if (availableTask == null) {
			// Moves if there are no pickup available
			City newCity = states.get(currentCity).get(null).bestAction;
			System.out.println("I am in " + currentCity.name + " and I decide to go to " + newCity.name);
			action = new Move(newCity);
		} else {
			// Decides whether to pickup or not the delivery
			City newCity = states.get(currentCity).get(availableTask.deliveryCity).bestAction;
			if(newCity.name == availableTask.deliveryCity.name){
				System.out.println("I take the pickup from " 
						+ availableTask.pickupCity.name + " to " 
						+ availableTask.deliveryCity.name + " for "
						+ availableTask.reward);
				action = new Pickup(availableTask);
			} else {
				System.out.println("I don't take the pickup from " 
						+ availableTask.pickupCity.name + " to " 
						+ availableTask.deliveryCity.name + " for "
						+ availableTask.reward + ". I got to " + newCity.name + " instead");
				action = new Move(newCity);
			}
		}

		if (numActions >= 1) {
			System.out.println(
					"The total profit after "+numActions+
					" actions is "+myAgent.getTotalProfit()+
					" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")\n"
					);
		}
		numActions++;

		return action;
	}
}
