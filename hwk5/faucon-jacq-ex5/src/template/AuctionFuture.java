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
public class AuctionFuture implements AuctionBehavior {

	private Agent agent;
	private Topology topology;
	private TaskDistribution distribution;

	private long timeout_plan;
	private long timeout_bid;
	private long timeout_setup;
	private int totalIncome = 0;
	private Random rand;

	private List<Task> taskList;
	private Planner ownPlan;
	private Planner ownPlanFuture;
	private Planner advPlan;
	private Planner advPlanFuture;

	private List<Task> possibleTasks;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
		System.out.println("#############################");
		System.out.println("SETUP FOR FUTURE AGENT");
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
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);

		this.agent = agent;
		this.topology = topology;
		this.distribution = distribution;

		rand = new Random();
		taskList = new ArrayList<Task>();
		ownPlan = new Planner(agent.vehicles(), new ArrayList<Task>());
		advPlan = new Planner(agent.vehicles(), new ArrayList<Task>());

		possibleTasks = new ArrayList<Task>();
		int taskId = 0;
		for (City from : topology.cities()) {
			for (City to : topology.cities()) {
				// logist.task.Task.Task(int id, City source, City destination,
				// long reward, int weight)
				possibleTasks.add(new Task(taskId++, from, to, 0, (int) ownPlan.maxCapacity / 2));
			}
		}

	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		System.out.println("#############################");
		System.out.println("AUCTION RESULT FOR FUTURE AGENT");
		System.out.println("AGENT ID: " + agent.id());
		System.out.println(bids[agent.id()] + " vs. " + bids[1 - agent.id()]);
		if (agent.id() == winner) {
			System.out.println("I WIN IT");
			totalIncome += bids[agent.id()];
			ownPlan = ownPlanFuture;
			System.out.println("NEW PLAN:");
			ownPlan.showPlan();
		} else {
			System.out.println("I LOSE IT...");
			advPlan = advPlanFuture;
		}
		System.out.println("#############################");
	}

	@Override
	public Long askPrice(Task x) {
		System.out.println("#############################");
		System.out.println("BIDING FOR FUTURE AGENT");
		System.out.println("FROM " + x.pickupCity + " TO " + x.deliveryCity);
		
		int nRandomTasks = 10;
		int aTime = (int) (timeout_bid * 0.8 / nRandomTasks * 0.25);
		
		ownPlanFuture = ownPlan.addTask(x);
		ownPlanFuture = ownPlanFuture.improve(aTime);
		advPlanFuture = advPlan.addTask(x);
		advPlanFuture = advPlanFuture.improve(aTime);

		double bid = 0;
		for (int i = 0; i < nRandomTasks; i++) {
			Task y = possibleTasks.get(rand.nextInt(possibleTasks.size()));
			Planner ownPlanY = ownPlan.addTask(y);
			ownPlanY = ownPlanY.improve(aTime);
			Planner advPlanY = advPlan.addTask(y);
			advPlanY = advPlanY.improve(aTime);
			Planner ownPlanXY = ownPlanFuture.addTask(y);
			ownPlanXY = ownPlanXY.improve(aTime);
			Planner advPlanXY = advPlanFuture.addTask(y);
			advPlanXY = advPlanXY.improve(aTime);

			double ownMarginalX = ownPlanFuture.cost - ownPlan.cost;
			double advMarginalX = advPlanFuture.cost - advPlan.cost;
			double ownMarginalY = ownPlanY.cost - ownPlan.cost;
			double advMarginalY = advPlanY.cost - advPlan.cost;
			double ownMarginalXY = ownPlanXY.cost - ownPlanFuture.cost;
			double advMarginalXY = advPlanXY.cost - advPlanFuture.cost;

			System.out.println("# # # # # # # # # # # #");
			// Plan P
			System.out.println("P = " + ownPlan.cost);
			System.out.println("P'= " + advPlan.cost);
			// Plan P+X
			System.out.println("P + X = " + ownPlanFuture.cost + "___" + ownMarginalX);
			System.out.println("P'+ X'= " + advPlanFuture.cost + "___" + advMarginalX);
			// Plan P+Y
			System.out.println("P + Y = " + ownPlanY.cost + "___" + ownMarginalY);
			System.out.println("P'+ Y'= " + advPlanY.cost + "___" + advMarginalY);
			// Plan P+X+Y
			System.out.println("P + X + Y = " + ownPlanXY.cost + "___" + ownMarginalXY);
			System.out.println("P'+ X'+ Y'= " + advPlanXY.cost + "___" + advMarginalXY);

			double middleBid = 0.5 * (ownMarginalX + advMarginalX);
			double futureGain = 0.5 * (advMarginalY - ownMarginalXY - (advMarginalXY - ownMarginalY));
			double subbid = middleBid - 0.5 * futureGain;
			System.out.println("MIDDLE: " + middleBid);
			System.out.println("FUTURE GAIN: " + futureGain);
			System.out.println("SUBBID: " + subbid);
			bid += subbid;
		}
		bid /= nRandomTasks;
		System.out.println("BID: " + bid);
		System.out.println("#############################");
		return (long) (bid + 0.9);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();
		
		ArrayList<Task> lt = new ArrayList<Task>();
		lt.addAll(tasks);
		ownPlan.tasks = lt;
		Planner Abest = ownPlan.improve((int) (timeout_plan*0.9));

		System.out.println("#############################");
		System.out.println("PLANNING RESULT FOR FUTURE AGENT");
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
