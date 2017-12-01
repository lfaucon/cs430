package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

public class Planner {

	public List<List<Integer>> plans;
	public double cost;

	public int bestVehicleIndex;
	public double maxCapacity;

	public List<Vehicle> vehicles;
	public List<Task> tasks;
	public int nTasks;
	public int nVehicles;

	static Random rand = new Random();

	// This generates the initial solution
	public Planner(List<Vehicle> vehicles, List<Task> tasks) {
		// Initialize the list of plans
		plans = new ArrayList<List<Integer>>();
		cost = 0.;
		// Add empty plans for the other vehicles
		for (Vehicle v : vehicles) {
			plans.add(new ArrayList<Integer>());
		}
		// Initialize by giving equal number of task to each vehicle
		// This does not matter so much
		nVehicles = vehicles.size();
		nTasks = tasks.size();
		this.vehicles = vehicles;
		this.tasks = tasks;
		// finds the best vehicle with the max capacity
		double maxCapacity = 0;
		int bestVehicleIndex = -1;
		for (int vindex = 0; vindex < nVehicles; vindex++) {
			if (vehicles.get(vindex).capacity() > maxCapacity) {
				maxCapacity = vehicles.get(vindex).capacity();
				bestVehicleIndex = vindex;
			}
		}

		// legal initialization: give everything to the largest vehicle
		for (int i = 1; i < nTasks + 1; i++) {
			if (tasks.get(i - 1).weight > maxCapacity)
				System.out.println("ERROR MAX WEIGHT EXCEEDS MAX CAPACITY");
			plans.get(bestVehicleIndex).add(i); // pickups
			plans.get(bestVehicleIndex).add(-i); // deliveries
		}

		for (int vIndex = 0; vIndex < nVehicles; vIndex++) {
			cost += getVehicleCost(vIndex);
		}
		
	}

	// This copies a solution
	public Planner(Planner solution) {
		plans = new ArrayList<List<Integer>>();
		// /!\ /!\ be careful this creates a new "plans" object
		// /!\ /!\ but keeps the previous lists objects
		for (List<Integer> p : solution.plans) {
			plans.add(p);
		}
		cost = solution.cost;
		bestVehicleIndex = solution.bestVehicleIndex;
		maxCapacity = solution.maxCapacity;
		nTasks = solution.nTasks;
		nVehicles = solution.nVehicles;
		vehicles = solution.vehicles;
		tasks = solution.tasks;
	}

	public boolean checkVehicleWeight(int vIndex, List<Vehicle> vehicles,
			List<Task> tasks) {
		double maxWeight = vehicles.get(vIndex).capacity();
		double currentWeight = 0.;
		for (int t : plans.get(vIndex)) {
			if (t > 0) {
				currentWeight += tasks.get(t - 1).weight;
				if (currentWeight > maxWeight)
					return false;
			} else {
				currentWeight -= tasks.get(-t - 1).weight;
			}
		}
		return true;
	}

	public double getVehicleCost(int vIndex) {
		return getVehiclePlan(vIndex).totalDistance()
				* vehicles.get(vIndex).costPerKm();
	}

	public Plan getVehiclePlan(int vIndex) {
		City current = vehicles.get(vIndex).homeCity();
		Plan plan = new Plan(current);

		for (int t : plans.get(vIndex)) {
			if (t > 0) { // pickup case
				Task pickupTask = tasks.get(t - 1);
				//System.out.println(current);
				for (City city : current.pathTo(pickupTask.pickupCity)) {
					plan.appendMove(city);
				}
				current = pickupTask.pickupCity;
				plan.appendPickup(pickupTask);
			} else { // delivery case
				Task deliveryTask = tasks.get(-t - 1);
				for (City city : current.pathTo(deliveryTask.deliveryCity)) {
					plan.appendMove(city);
				}
				current = deliveryTask.deliveryCity;
				plan.appendDelivery(deliveryTask);
			}
		}
		return plan;
	}

	public List<Plan> getPlan() {
		List<Plan> getPlans = new ArrayList<Plan>();

		for (int v = 0; v < vehicles.size(); v++) {
			getPlans.add(getVehiclePlan(v));
		}

		return getPlans;
	}

	public void showPlan() {
		System.out.println("#############################");
		System.out.println("PLAN:");
		for (List<Integer> li : plans) {
			System.out.println(li);
		}
		System.out.println("#############################");
	}

	// Copies and add a task to a Planner object
	public Planner addTask(Task task) {
		// Copies the current plan
		Planner newPlanner = new Planner(this);
		List<Integer> bVPlan = newPlanner.plans.get(bestVehicleIndex);
		// Increase task count
		newPlanner.nTasks += 1;
		// Add new task to the plan of max capacity vehicle
		List<Integer> newPlan = new ArrayList<Integer>();
		newPlan.addAll(bVPlan);
		newPlan.add(newPlanner.nTasks);
		newPlan.add(-newPlanner.nTasks);
		// Add task to list of tasks
		newPlanner.plans.set(bestVehicleIndex, newPlan);
		newPlanner.tasks = new ArrayList<Task>();
		newPlanner.tasks.addAll(tasks);
		newPlanner.tasks.add(task);
		// Recompute cost
		newPlanner.cost += newPlanner.getVehicleCost(bestVehicleIndex)
				- this.getVehicleCost(bestVehicleIndex);
		return newPlanner;
	}

	static Planner localChoice(List<Planner> ls, double p) {
		// With probability p we pick a solution at random
		if (rand.nextDouble() < p)
			return ls.get(rand.nextInt(ls.size()));
		// With probability 1-p we pick the best solution
		Planner sol = ls.get(0);
		for (Planner s : ls) {
			if (s.cost < sol.cost)
				sol = s;
		}
		return sol;
	}

	public List<Planner> getNeighbours() {
		// Initialize the list of neighbors
		List<Planner> neighbors = new ArrayList<Planner>();
		// Add the old solution to the neighbors
		neighbors.add(this);

		if (nTasks < 1)
			return neighbors;
		// Picks at random a vehicle that will be removed one task
		int randTaskIndex = 1 + rand.nextInt(nTasks);
		int randVehicle1 = -1;
		for (int vIndex = 0; vIndex < nVehicles; vIndex++) {
			if (this.plans.get(vIndex).contains(randTaskIndex))
				randVehicle1 = vIndex;
		}
		double costV1with = this.getVehicleCost(randVehicle1);

		// Makes a copy of the current solution, then remove from v1 the random
		// task
		Planner sCopy = new Planner(this);
		List<Integer> lv1 = new ArrayList<Integer>();
		lv1.addAll(sCopy.plans.get(randVehicle1));
		lv1.remove(lv1.indexOf(randTaskIndex));
		lv1.remove(lv1.indexOf(-randTaskIndex));
		sCopy.plans.set(randVehicle1, lv1);
		double costV1without = sCopy.getVehicleCost(randVehicle1);
		// picks at random a second vehicle that will have to handle the task
		int randVehicle2 = rand.nextInt(nVehicles);
		List<Integer> v2Plan = sCopy.plans.get(randVehicle2);
		double costV2without = sCopy.getVehicleCost(randVehicle2);

		// for each spot where we can add the pickup and delivery (p/d-Indexes)
		// we create a neighbor
		for (int pIndex = 0; pIndex < v2Plan.size() + 1; pIndex++) {
			for (int dIndex = pIndex + 1; dIndex < v2Plan.size() + 2; dIndex++) {
				// System.out.println("(p="+pIndex+",d="+dIndex+")"+v2Plan.size());
				Planner newSolution = new Planner(sCopy);
				List<Integer> newV2Plan = new ArrayList<Integer>();
				newSolution.plans.set(randVehicle2, newV2Plan);
				newV2Plan.addAll(v2Plan);
				newV2Plan.add(pIndex, randTaskIndex);
				newV2Plan.add(dIndex, -randTaskIndex);
				// compute correct cost for newV2Plan
				double costV2with = newSolution.getVehicleCost(randVehicle2);
				newSolution.cost += costV2with - costV2without - costV1with
						+ costV1without;
				// Add the neighbor only if it satisfies the weight constraints
				if (newSolution.checkVehicleWeight(randVehicle2, vehicles,
						tasks)) {
					// newSolution.showPlan();
					neighbors.add(newSolution);
				}
			}
		}

		return neighbors;
	}

	public Planner improve(long time) {
		long timeLimit = System.currentTimeMillis() + time;
		Planner A = this;
		Planner Abest = A;
		// Initialize the temperature parameter
		double T = 1;
		// Stochastic Local Search Algorithm
		while (System.currentTimeMillis() < timeLimit) {
			List<Planner> N = A.getNeighbours();
			A = Planner.localChoice(N, 1. / Math.sqrt(T));
			// Updates the temperature
			T += 0.5;
			if (A.cost < Abest.cost)
				Abest = A;
		}
		// returns overall best plan
		return Abest;
	}

	static Planner sLS(List<Vehicle> vehicles, ArrayList<Task> tasks, long time) {
		Planner A = new Planner(vehicles, tasks);
		if (tasks.size() < 1)
			return A;
		return A.improve(time);
	}
}
