package template;

import java.util.ArrayList;
import java.util.List;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class Solution {

	public List<List<Integer>> plans;
	public double cost;
	boolean possible;

	// This generates the initial solution
	public Solution (List<Vehicle> vehicles, List<Task> tasks) {
		// Initialize the list of plans
		plans = new ArrayList<List<Integer>>();
		cost = 0.;
		possible = true;
		

		// Add empty plans for the other vehicles
		while (plans.size() < vehicles.size()) {
			plans.add(new ArrayList<Integer>());
		}

		// Initialize by giving equal number of task to each vehicle
		// This does not matter so much
		int nVehicles = vehicles.size();
		int nTasks = tasks.size();
		
		double MAX_CAPACITY = 0;
		int BEST_VEHICLE_IDX = -1;
		for (int vindex =0; vindex<nVehicles; vindex++) {
			if (vehicles.get(vindex).capacity()>MAX_CAPACITY) {
				MAX_CAPACITY = vehicles.get(vindex).capacity();
				BEST_VEHICLE_IDX = vindex;
			}
		}
		
		double MAX_TASK_WEIGHT = 0;
		for (int tindex = 0; tindex<nTasks; tindex++) {
			if (tasks.get(tindex).weight>MAX_TASK_WEIGHT) MAX_TASK_WEIGHT = tasks.get(tindex).weight;
		}
		
		if (MAX_CAPACITY<MAX_TASK_WEIGHT) possible = false;
		
		// legal initialization: give everything to the largest vehicle
		for (int i = 0; i < nTasks; i++){
			plans.get(BEST_VEHICLE_IDX).add(i); // pickups
			plans.get(BEST_VEHICLE_IDX).add(nTasks+i); //deliveries
		}

		for (int vIndex = 0; vIndex < nVehicles; vIndex++) {
			cost += getVehicleCost(vIndex, vehicles, tasks);
		}
	}

	// This copies a solution
	public Solution(Solution solution) {
		plans = new ArrayList<List<Integer>>();
		// /!\ /!\ be careful this creates a new "plans" object
		// /!\ /!\ but keeps the previous lists objects
		for (List<Integer> p: solution.plans) {
			plans.add(p);
		}
		cost = solution.cost;
	}

	public boolean checkVehicleWeight(int vIndex, List<Vehicle> vehicles, List<Task> tasks) { 
		int nTasks = tasks.size();
		double MAX_WEIGHT = vehicles.get(vIndex).capacity();
		double currentWeight = 0.;
		for(int t: plans.get(vIndex)){
			if(t < nTasks){
				currentWeight += tasks.get(t).weight;
				if(currentWeight > MAX_WEIGHT) return false;
			} else {
				currentWeight -= tasks.get(t-nTasks).weight;
			}
		}
		return true;
	}

	public double getVehicleCost(int vIndex, List<Vehicle> vehicles, List<Task> tasks) {
		return getVehiclePlan(vIndex, vehicles, tasks).totalDistance() * vehicles.get(vIndex).costPerKm();
	}

	public Plan getVehiclePlan(int vIndex, List<Vehicle> vehicles, List<Task> tasks){
		int nTasks = tasks.size();
		City current = vehicles.get(vIndex).getCurrentCity();
		Plan plan = new Plan(current);

		for (int t: plans.get(vIndex)) {
			if(t < nTasks){ // pickup case
				for (City city : current.pathTo(tasks.get(t).pickupCity)) {
					plan.appendMove(city);
				}
				current = tasks.get(t).pickupCity;
				plan.appendPickup(tasks.get(t));
			} else { // delivery case
				for (City city : current.pathTo(tasks.get(t-nTasks).deliveryCity)) {
					plan.appendMove(city);
				}
				current = tasks.get(t-nTasks).deliveryCity;
				plan.appendDelivery(tasks.get(t-nTasks));
			}
		}
		return plan;
	}

	public List<Plan> getPlan(List<Vehicle> vehicles, List<Task> tasks) {
		List<Plan> getPlans = new ArrayList<Plan>();

		for(int v=0; v<vehicles.size(); v++){
			getPlans.add(getVehiclePlan(v,vehicles, tasks));
		}

		return getPlans;
	}

	public void showPlan() {
		System.out.println("#############################");
		System.out.println("PLAN:");
		for (List<Integer> li: plans) {
			System.out.println(li);
		}
		System.out.println("#############################");
	}

}

