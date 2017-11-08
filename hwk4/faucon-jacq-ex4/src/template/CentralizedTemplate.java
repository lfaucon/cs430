package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup;
	private long timeout_plan;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config/settings_default.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();

		List<Task> lt = new ArrayList<Task>();
		for(Task t: tasks) lt.add(t);

		Solution A = new Solution(vehicles, lt);
		if (!A.possible) {
			System.out.println("no possible solution");
			return A.getPlan(vehicles, lt);
		}
		Solution Abest = A;

		System.out.println("#############################");
		System.out.println("ITERATION: " + 0);
		System.out.println("SOLUTION COST : " + A.cost);
		System.out.println("#############################");

		// Initialize the temperature parameter
		double T = 1;

		// Stochastic Local Search Algorithm
		for (int i = 0; i < 1e5; i++) {
			System.out.println("#############################");
			System.out.println("ITERATION: " + i);

			Solution Aold = A;

			List<Solution> N = ChooseNeighbours.get(Aold, vehicles, lt);

			A = LocalChoice.get(N, 1. / Math.sqrt(T));
			// Updates the temperature
			T += 0.5;

			if(A.cost < Abest.cost) Abest = A;
			System.out.println("SOLUTION COST : " + A.cost);
			System.out.println("#############################");	
			
			
			if(System.currentTimeMillis() - time_start > timeout_plan - 5000) {
				System.out.println("Stopping for timeout");
				break;
			}
		}

		System.out.println("#############################");
		System.out.println("FINAL SOLUTION COST : " + Abest.cost);
		System.out.println("#############################");


		Abest.showPlan();
		List<Plan> lp = Abest.getPlan(vehicles, lt);

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in "+duration+" milliseconds.");

		return lp;
	}

}

