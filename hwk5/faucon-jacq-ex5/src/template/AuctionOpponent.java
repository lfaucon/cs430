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
public class AuctionOpponent implements AuctionBehavior {

	private Agent agent;
	private Topology topology;
	private long timeout_plan;
	private long timeout_bid;
	private long timeout_setup;

	private int totalIncome = 0;
	private Random rand = new Random();

	private List<Task> taskList;
	private Planner currentPlan;
	private Planner futurePlan;
	private List<Planner> currentAdv;
	private List<Planner> futureAdv;

	private double M;

	private int nAdv = 3;
	private int nRandomTasks = 20;
	private int nSample = 5;
	
	private int iterCount = 0;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
		System.out.println("#############################");
		System.out.println("SETUP FOR OPPONENT AGENT");
		System.out.println("#############################");

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config/settings_auction.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the config file.");
		}
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);

		this.agent = agent;
		this.topology = topology;

		taskList = new ArrayList<Task>();
		currentPlan = new Planner(agent.vehicles(), new ArrayList<Task>());

		double advCapacity = 0.;
		double advCostPerKm = 0.;
		for (Vehicle v : agent.vehicles()) {
			advCapacity += (double) v.capacity() / agent.vehicles().size();
			advCostPerKm += (double) v.costPerKm() / agent.vehicles().size();
		}

		currentAdv = new ArrayList<Planner>();
		int randCityIndex = 0;
		for (int adv = 0; adv < nAdv; adv++) {
			List<Vehicle> advVehicles = new ArrayList<Vehicle>();
			for (int nV = 0; nV < currentPlan.nVehicles; nV++) {
				City advCity = topology.cities().get(randCityIndex++ % topology.size());
				advVehicles.add(new VehicleBis((int) Math.ceil(advCapacity),
						(int) Math.floor(advCostPerKm), advCity));
			}
			currentAdv.add(new Planner(advVehicles, new ArrayList<Task>()));
		}

		M = 0.;
		List<Task> possibleTasks = new ArrayList<Task>();
		int taskId = 0;
		for (City from : topology.cities()) {
			for (City to : topology.cities()) {
				possibleTasks.add(new Task(taskId++, from, to, 0, 0));
			}
		}
		Planner[] plans = new Planner[nSample];
		for (int i = 0; i < nSample; i++) {
			plans[i] = new Planner(agent.vehicles(), new ArrayList<Task>());
			for (int k = 1; k <= nRandomTasks; k++) {
				Task y = possibleTasks.get(rand.nextInt(possibleTasks.size()));
				plans[i] = plans[i].addTask(y);
			}
			plans[i] = plans[i].improve((int) (0.9 * timeout_setup / nSample));
			M += plans[i].cost / nRandomTasks / nSample;
		}
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		System.out.println("#############################");
		System.out.println("AUCTION RESULT FOR OPPONENT AGENT");
		System.out.println("AGENT ID: " + agent.id());
		if (agent.id() == winner) {
			System.out.println("I WIN IT");
			totalIncome += bids[agent.id()];
			currentPlan = futurePlan.improve((int) (0.1 * timeout_bid));
			System.out.println("NEW PLAN:");
			currentPlan.showPlan();
		} else {
			System.out.println("I LOSE IT...");
			currentAdv = futureAdv;
		}
		System.out.println("#############################");
	}

	@Override
	public Long askPrice(Task t) {
		System.out.println("#############################");
		System.out.println("BIDING FOR OPPONENT AGENT");
		System.out.println("FROM " + t.pickupCity + " TO " + t.deliveryCity);
		futurePlan = currentPlan.addTask(t);
		futurePlan = futurePlan.improve((int) (timeout_bid * 0.2));

		System.out.println("# # # # # # # # # # # # # # #");
		futureAdv = new ArrayList<Planner>();
		double advMarginalCost = 0;
		for (Planner advPlan : currentAdv) {
			Planner fAdvPlan = advPlan.addTask(t);
			fAdvPlan = fAdvPlan.improve((int) (timeout_bid * 0.6 / nAdv));
			advMarginalCost += (fAdvPlan.cost - advPlan.cost) / nAdv;
			futureAdv.add(fAdvPlan);
			System.out.println("ADV NTASKS: " + fAdvPlan.nTasks);
			System.out.println("ADV OLD COST: " + advPlan.cost);
			System.out.println("ADV NEW COST: " + fAdvPlan.cost);
			System.out.println("ADV MARG COST: "
					+ (fAdvPlan.cost - advPlan.cost));
			System.out.println("# # # # # # # # # # # # # # #");
		}
		System.out.println("ADV MARG COST: " + advMarginalCost);
		System.out.println("# # # # # # # # # # # # # # #");

		System.out.println("NTASKS: " + futurePlan.nTasks);
		System.out.println("CURRENT COST: " + currentPlan.cost);
		System.out.println("FUTURE COST: " + futurePlan.cost);

		double marginal = Math.max(0, futurePlan.cost - currentPlan.cost);
		System.out.println("MARGINAL COST: " + marginal);

		double bidOp = (0.5 * (marginal + advMarginalCost));
		System.out.println("OP STRATEGY BID: " + bidOp);
		System.out.println("#############################");

		double bidStart = 0.;
		bidStart = M;
		System.out.println("START STRATEGY BID: " + bidStart);
		System.out.println("#############################");
		
		iterCount++;
		double e = Math.exp(0.5 * (10 - iterCount));
		double gamma = 1 / (1 + e);

		long bid = (long) ((1 - gamma) * bidStart + gamma * (100 + bidOp));
		System.out.println("GAMMA: " + gamma);
		System.out.println("MIXED BID: " + bid);
		return bid;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();

		ArrayList<Task> lt = new ArrayList<Task>();
		lt.addAll(tasks);
		currentPlan.tasks = lt;
		Planner Abest = currentPlan.improve((int) (timeout_plan * 0.9));

		System.out.println("#############################");
		System.out.println("PLANNING RESULT FOR OPPONENT AGENT");
		System.out.println("SOLUTION COST: " + Abest.cost);
		System.out.println("TOTAL INCOMES: " + totalIncome);
		System.out.println("TOTAL BENEFIT: " + (totalIncome - Abest.cost));
		System.out.println("#############################");

		Abest.showPlan();
		List<Plan> lp = Abest.getPlan();
		double cost = 0;
		for (int vIndex = 0; vIndex < Abest.nVehicles; vIndex++) {
			cost += lp.get(vIndex).totalDistance()
					* vehicles.get(vIndex).costPerKm();
		}
		System.out.println("CHECK COST: " + cost);

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration
				+ " milliseconds.");

		return lp;
	}
}
