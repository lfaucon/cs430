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
public class AuctionUniform implements AuctionBehavior {
	
	private Agent agent;
	private long timeout_plan;
	private int totalIncome = 0;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		System.out.println("#############################");
		System.out.println("SETUP FOR UNIFORM AGENT");
		System.out.println("#############################");
		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config/settings_auction.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		this.agent = agent;
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		System.out.println(agent.id()+" ME: "+bids[agent.id()]+" OP: "+bids[1-agent.id()]+" WINNER: "+winner);
		if(agent.id() == winner) totalIncome += bids[agent.id()];
	}
	
	@Override
	public Long askPrice(Task task) {
		return 1000L;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();
		
		ArrayList<Task> lt = new ArrayList<Task>();
		for(Task t: tasks) lt.add(t);
		
		Planner Abest = Planner.sLS(vehicles, lt, (int) (timeout_plan*0.9));

		System.out.println("#############################");
		System.out.println("RESULT FOR UNIFORM AGENT");
		System.out.println("SOLUTION COST: " + Abest.cost);
		System.out.println("TOTAL INCOMES: " + totalIncome);
		System.out.println("TOTAL BENEFIT: " + (totalIncome - Abest.cost));
		System.out.println("#############################");

		Abest.showPlan();
		List<Plan> lp = Abest.getPlan();

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in "+duration+" milliseconds.");

		return lp;
	}
}
