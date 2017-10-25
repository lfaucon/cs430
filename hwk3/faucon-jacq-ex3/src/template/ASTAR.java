package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;

import logist.plan.Plan;

public class ASTAR {

	public static Plan getPlan(Vehicle vehicle, TaskSet tasks) {
		System.out.println("Computing ASTAR plan. . .");
		City currentCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currentCity);
		
		// Create the initial state
		State state = new State(currentCity);
		// Add tasks for pickup and tasks for delivery
		state.restTasks.addAll(tasks);
		state.currentTasks.addAll(vehicle.getCurrentTasks());
		
		PriorityQueue<State> Q = new PriorityQueue<State>();
		HashMap<State,Double> H = new HashMap<State,Double>();
		Q.offer(state);
		H.put(state, state.cost);
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
			state.cost = H.get(state);
			
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
						// g is the distance in kilometers and h is out heuristic
						newState.cost = newState.g + h(newState, vehicle);
						offerState(Q, H, fatherState, fatherAction, new Action(pickup, null), state, newState);
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
					// g is the distance in kilometers and h is out heuristic
					newState.cost = newState.g + h(newState, vehicle);
					offerState(Q, H, fatherState, fatherAction, new Action(null, deliver), state, newState);
				}
			}
		}

		// Build the plan
		if(!goalReached) {
			System.out.println("no goal found");
		} else {
			System.out.println("goal found with cost "+state.cost+" planification...");
			Stack<Action> reversePlan = new Stack<Action>();

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
	
	private static void offerState(Queue<State> Q, HashMap<State,Double> H, HashMap<State,State> fatherState, HashMap<State,Action> fatherAction, Action action, State state, State newState) {
		if(H.containsKey(newState)) {
			if(H.get(newState) > newState.cost) {
				H.put(newState, newState.cost);
				fatherState.put(newState, state);
				fatherAction.put(newState, action);
				Q.remove(newState);
				Q.add(newState);
			}	
		} else {
			Q.offer(newState);
			H.put(newState, newState.cost);
			fatherState.put(newState, state);
			fatherAction.put(newState, action);
		}
	}
	
	private static double h(State state, Vehicle vehicle) {
		//System.out.println("################################\nCOMPUTING h for "+state.restTasks.size()+" remaining tasks");
		
		// Computes h as the distance to the minumum furthest city
		double h = 0.;
		for(Task task: state.restTasks) {
			double x = state.currentCity.distanceTo(task.pickupCity) + task.pickupCity.distanceTo(task.deliveryCity);
			if(h < x) h = x;
		}
		
		// Computes h as the minimum of several random trials
		/*
		int nIter = (int) Math.min(Math.pow(5,state.restTasks.size()), 100);
		double h = -1;
		
		for (int iter = 0; iter < nIter; iter++) {
			ArrayList<Task> tsks = new ArrayList<Task>(state.restTasks);
			Collections.shuffle(tsks);
			Plan plan = NAIVE.getPlan(vehicle, tsks);
			System.out.println("ITER "+iter+" DISTANCE "+plan.totalDistance());
			if(h < 0 | h > plan.totalDistance()){
				h = plan.totalDistance();
			}
		}
		h *= 0.8
		// */
		//System.out.println("H " + h);
		return h;
	}
}
