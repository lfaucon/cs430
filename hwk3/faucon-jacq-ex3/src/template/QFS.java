package template;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Stack;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class QFS {

	public static Plan getPlan(Vehicle vehicle, TaskSet tasks) {
		System.out.println("Computing BFS plan. . .");
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

		for(int step=0; step<1e7; step++) {
			if(step % 1e4 == 0) System.out.println("step: " + step);

			if(Q.isEmpty()) {
				System.out.println("empty tree after "+step+" iterations");
				break;
			}


			state = Q.poll();
			// If there are no more task to pickup and no task to deliver, then we terminate
			if(state.restTasks.isEmpty() && state.currentTasks.isEmpty()) {
				// first goal reached necessarily the one with optimal cost
				System.out.println("goal found after "+step+" iterations");
				goalReached = true;
				break;
			}

			boolean delivers_closer = false;
			// Create a new branch for each task that need to be picked-up
			for(Task pickup : state.restTasks) {// probably easy to optimize this double loop:
				delivers_closer = false;

				for(Task tsk : state.currentTasks) {
					if(state.currentCity.pathTo(pickup.pickupCity).contains(tsk.deliveryCity)) {
						delivers_closer = true; // passing a city where we can deliver is idiot
					}
				}

				if(!delivers_closer) {
					if(vehicle.capacity()>state.weight+pickup.weight) {
						State newState = DeliberativeTemplate.getNewState(state, pickup, null);
						Q.offer(newState);
						fatherState.put(newState, state);
						fatherAction.put(newState, new Action(pickup, null));
					}
				}
			}

			// Create a new branch for each task that can be delivered
			delivers_closer = false; 
			for(Task deliver : state.currentTasks) {// probably easy to optimize this double loop:
				delivers_closer = false;

				for(Task tsk : state.currentTasks) {
					if(deliver.deliveryCity != tsk.deliveryCity && 
							state.currentCity.pathTo(deliver.deliveryCity).contains(tsk.deliveryCity)) {
						delivers_closer = true; // passing a city where we can deliver is idiot
					}
				}

				if(!delivers_closer) {
					State newState = DeliberativeTemplate.getNewState(state, null, deliver);
					Q.offer(newState);
					fatherState.put(newState, state);
					fatherAction.put(newState, new Action(null, deliver));
				}
			}
		}

		// Build the plan
		if(!goalReached) {
			System.out.println("no goal found");
		} else {
			System.out.println("goal found with cost "+state.cost+" planification...");
			Stack<Action> reversePlan = new Stack<Action>();
			int i = 0;

			while(state != null) {
				if(fatherState.get(state)!=null) {
					//System.out.println("state city "+state.currentCity+" father state "+fatherState.get(state).currentCity);
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
}
