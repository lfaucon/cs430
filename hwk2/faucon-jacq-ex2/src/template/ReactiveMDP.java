package template;

import java.util.HashMap;
import java.util.List;

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

public class ReactiveMDP implements ReactiveBehavior {

	private int numActions;
	private Agent myAgent;
	private float conv;

	private HashMap<City, HashMap<City, State>> states;
	

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		int costPerKm = agent.vehicles().get(0).costPerKm();

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

		for(int step=0; step<1000; step++){
			//System.out.println(step + "___" + discount);
			this.conv = 0;
			for(HashMap<City, State> hm: states.values()){ // for cityfrom's hm of cityto
				for(State state: hm.values()){ // for s : cityfrom,cityto
					state.updateValues(discount, topology.cities(), states, td); //q(a,s) <- R(a,s) + gama sum_s' P(s'|a,s)*max_a'(q(a',s))
				}
			}
			for(HashMap<City, State> hm: states.values()){
				for(State state: hm.values()){
					double oldbestvalue = state.bestValue;
					state.updateBestAction(); // so we know max_a(q(a,s)) forall s
					double newbestvalue = state.bestValue;
					this.conv += (oldbestvalue - newbestvalue)*(oldbestvalue - newbestvalue);
				}
			}
			System.out.println(step + "___" + discount + "conv = " + this.conv);
			if(this.conv<1E-6) {
				break;
			}
		}
	}

	// Define the class state
	public class State {

		public City cityFrom;
		public City cityTo;

		// The computation of the expected value Q(a,s)
		private HashMap<City,Double> values;
		// the immediate reward for each action R(a;s)
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
				rewards.put(neighbor, (- from.distanceTo(neighbor) * costPerKm)/100000.); 
			}
			// delivering action
			if(cityTo != null){
				values.put(to, 0.);
				rewards.put(to, (td.reward(from, to) - from.distanceTo(to) * costPerKm)/100000.);
			}
		}

		public void updateValues(double discount, List<City> cities, HashMap<City, HashMap<City, State>> states, TaskDistribution td){
			// For each action (corresponding to a new city)
			for(City newCity: values.keySet()){// for action : newcity
				// initialize the value with the immediate reward
				double newValue = rewards.get(newCity);

				double noTaskProbability = 1.;
				for(City cityTo: cities){// for all s' : cityfrom==newcity,cityto
					newValue += discount * td.probability(newCity,cityTo) * states.get(newCity).get(cityTo).bestValue;
					noTaskProbability -= td.probability(newCity,cityTo);
				}
				// s' : cityfrom==newcity,notask
				newValue += discount * noTaskProbability * states.get(newCity).get(null).bestValue;

				// updates value table
				values.put(newCity, newValue);
			}
		}

		public void updateBestAction() {
			double maxValue = -100000.;
			City maxAction = (City) values.keySet().toArray()[0];
			for(City a: values.keySet()){ // for action newcity
				double q = values.get(a); // q(s,a)
				if(q > maxValue){
					maxValue = q;
					maxAction = a;
				}
			}
			bestValue = maxValue;
			bestAction = maxAction;
		}
	}



	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		// print agent name
		System.out.println(myAgent.name());
		
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
					"AVERAGE PROFIT: " + (myAgent.getTotalProfit() / (double)numActions) + "\n"
					);
		}
		numActions++;

		return action;
	}
}
