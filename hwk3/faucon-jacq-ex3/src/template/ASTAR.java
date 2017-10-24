package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;
import logist.plan.Plan;

public class ASTAR {

	public static Plan getPlan(Vehicle vehicle, TaskSet tasks) {
		// tasks = set of (pickupcity;deliverycity)
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		Double cost = 0.;

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);
			cost += current.distanceTo(task.pickupCity);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);
			cost += task.pickupCity.distanceTo(task.deliveryCity);

			// set current city
			current = task.deliveryCity;
		}
		System.out.println("found plan with cost " + cost);
		return plan;
	}

}
