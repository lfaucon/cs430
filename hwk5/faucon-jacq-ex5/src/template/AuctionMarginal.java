package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
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
public class AuctionMarginal implements AuctionBehavior {

	private Agent agent;
	private long timeout_plan;
	private long timeout_bid;
	private int totalIncome = 0;
	private Random rand;

	private List<Task> taskList;
	private Planner currentPlan;
	private Planner futurePlan;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
		System.out.println("#############################");
		System.out.println("SETUP FOR MARGINAL AGENT");
		System.out.println("#############################");
		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config/settings_auction.xml");
		} catch (Exception exc) {
			System.out
					.println("There was a problem loading the configuration file.");
		}
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
		this.agent = agent;
		rand = new Random();

		taskList = new ArrayList<Task>();
		currentPlan = new Planner(agent.vehicles(), new ArrayList<Task>());
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		System.out.println("#############################");
		System.out.println("AUCTION RESULT FOR MARGINAL AGENT");
		System.out.println("AGENT ID: " + agent.id());
		System.out.println(bids[agent.id()] + " vs. " + bids[1 - agent.id()]);
		if (agent.id() == winner) {
			System.out.println("I WIN IT");
			totalIncome += bids[agent.id()];
			currentPlan = futurePlan;
			System.out.println("NEW PLAN:");
			currentPlan.showPlan();
		} else {
			System.out.println("I LOSE IT...");
		}
		System.out.println("#############################");
	}

	@Override
	public Long askPrice(Task task) {
		System.out.println("#############################");
		System.out.println("BIDING FOR MARGINAL AGENT");
		System.out.println("FROM " + task.pickupCity + " TO "
				+ task.deliveryCity);
		System.out.println("CURRENT COST: " + currentPlan.cost);
		futurePlan = currentPlan.addTask(task);
		futurePlan = futurePlan.improve((int) (timeout_bid * 0.9));
		System.out.println("FUTURE COST: " + futurePlan.cost);
		long bid = (long) (futurePlan.cost - currentPlan.cost);
		System.out.println("BID: " + bid);
		System.out.println("#############################");
		return bid;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();

		ArrayList<Task> lt = new ArrayList<Task>();
		lt.addAll(tasks);
		currentPlan.tasks = lt;
		Planner Abest = currentPlan.improve((int) (timeout_plan*0.9));

		
		System.out.println("#############################");
		System.out.println("PLANNING RESULT FOR MARGINAL AGENT");
		System.out.println("SOLUTION COST: " + Abest.cost);
		System.out.println("TOTAL INCOMES: " + totalIncome);
		System.out.println("TOTAL BENEFIT: " + (totalIncome - Abest.cost));
		System.out.println("#############################");

		Abest.showPlan();
		List<Plan> lp = Abest.getPlan();
		double cost = 0;
		for (int vIndex = 0; vIndex < Abest.nVehicles; vIndex++) {
			cost += lp.get(vIndex).totalDistance() * vehicles.get(vIndex).costPerKm();
		}
		System.out.println("CHECK COST: " + cost);

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration
				+ " milliseconds.");

		return lp;
	}
}
