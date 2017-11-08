package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

public class ChooseNeighbours {

	static Random rand = new Random();

	static List<Solution> get(Solution solution, List<Vehicle> vehicles, List<Task> tasks) {

		System.out.println("#############################");
		System.out.println("Computing neighbors....");
		// initialise the list of neighbors
		List<Solution> neighbors = new ArrayList<Solution>();
		// Add the old solution to the neighbors
		neighbors.add(solution);
		
		int nVehicles = vehicles.size();
		int nTasks = tasks.size();

		// Picks at a vehicle that will be removed one task
		int randTaskIndex = rand.nextInt(nTasks);
		System.out.println("Task: "+randTaskIndex);

		int randVehicle1 = -1;
		for (int vIndex=0; vIndex < nVehicles; vIndex++) {
			if(solution.plans.get(vIndex).contains(randTaskIndex)) randVehicle1 = vIndex;
		}
		System.out.println("Vehicle1: "+randVehicle1);

		double costV1with = solution.getVehicleCost(randVehicle1, vehicles, tasks);

		// Makes a copy of the current solution, then remove from v1 the random task
		Solution sCopy = new Solution(solution);
		List<Integer> lv1 = new ArrayList<Integer>();
		lv1.addAll(sCopy.plans.get(randVehicle1));
		lv1.remove(lv1.indexOf(randTaskIndex));
		lv1.remove(lv1.indexOf(randTaskIndex+nTasks));
		sCopy.plans.set(randVehicle1, lv1);

		double costV1without = sCopy.getVehicleCost(randVehicle1, vehicles, tasks);

		// picks at random a second vehicle that will have to handle the task
		int randVehicle2 = rand.nextInt(nVehicles);
		List<Integer> v2Plan = sCopy.plans.get(randVehicle2);
		System.out.println("Vehicle2: "+randVehicle2);

		double costV2without = sCopy.getVehicleCost(randVehicle2, vehicles, tasks);

		// for each spot where we can add the pickup and delivery (p/d-Indexes)
		// we create a neighbor
		for (int pIndex=0; pIndex < v2Plan.size() + 1; pIndex++) {
			for (int dIndex=pIndex+1; dIndex < v2Plan.size() + 2; dIndex++) {
				//System.out.println("(p="+pIndex+",d="+dIndex+")"+v2Plan.size());

				Solution newSolution = new Solution(sCopy);
				List<Integer> newV2Plan = new ArrayList<Integer>();
				newSolution.plans.set(randVehicle2, newV2Plan);
				newV2Plan.addAll(v2Plan);
				newV2Plan.add(pIndex, randTaskIndex);
				newV2Plan.add(dIndex, randTaskIndex + nTasks);

				// compute correct cost for newV2Plan
				double costV2with = newSolution.getVehicleCost(randVehicle2, vehicles, tasks);
				newSolution.cost += costV2with - costV2without - costV1with + costV1without;

				// Add the neighbor only if it satisfies the weigh constraints
				if(newSolution.checkVehicleWeight(randVehicle2, vehicles, tasks)){
					//newSolution.showPlan();
					neighbors.add(newSolution);
				}
			}
		}

		System.out.println("#############################");
		return neighbors;
	}

}

