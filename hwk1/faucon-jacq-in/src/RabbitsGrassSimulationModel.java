import java.awt.Color;
import java.util.ArrayList;
import java.lang.Thread;

import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;


/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {

	// Default Values
	private static final int NUMRABBITS = 20;
	private static final int RABBITSTOMACH = 20;
	private static final int WORLDXSIZE = 20;
	private static final int WORLDYSIZE = 20;	
	private static final int GRASSGROWTH = 20;
	private static final int SLOWDOWN = 0;

	private int numAgents = NUMRABBITS;
	private int rabbitStomach = RABBITSTOMACH;
	private int worldXSize = WORLDXSIZE;
	private int worldYSize = WORLDYSIZE;
	private int grassGrowth = GRASSGROWTH;
	private int slowDown = SLOWDOWN;

	private Schedule schedule;
	private RabbitsGrassSimulationSpace rgSpace;
	private DisplaySurface displaySurf;
	private ArrayList<RabbitsGrassSimulationAgent> agentList;
        private OpenSequenceGraph graph;

	public static void main(String[] args) {
		System.out.println("Rabbits! Go!");
		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		init.loadModel(model, "", false);
	}

	public void begin() {
		System.out.println("Running begin");
		buildModel();
                setGraph();
		buildSchedule();
                graph.display();
		buildDisplay();
		displaySurf.display();
	}

	public String[] getInitParam() {
		String[] initParams = { "NumAgents", "RabbitStomach", "WorldXSize", "WorldYSize", "GrassGrowth", "SlowDown"};
		return initParams;
	}

	public String getName() {
		return "Model Of Rabbits";
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setup() {
		// System.out.println("Running setup");
		rgSpace = null;
		agentList = new ArrayList();
		schedule = new Schedule(1);		
		if (displaySurf != null){
			displaySurf.dispose();
		}
		displaySurf = null;
		displaySurf = new DisplaySurface(this, "Rabbit Grass Model Window 1");
		registerDisplaySurface("Rabbit Grass Model Window 1", displaySurf);
	}

	public void buildModel(){
		System.out.println("Running BuildModel");
		rgSpace = new RabbitsGrassSimulationSpace(worldXSize, worldYSize);
		for(int i = 0; i < numAgents; i++){
			addNewAgent();
		}
		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
			rga.report();
		}
	}

        private void setGraph() {
		graph = new OpenSequenceGraph("Agent Stats.", this);
		graph.setXRange(0, 200);
		graph.setYRange(0, 200);
		graph.setAxisTitles("time", "agent attributes");
		
		Sequence sec = new Sequence() {
			public double getSValue() {
			    return countLivingAgents();
			}
		};
		
		graph.addSequence("population", sec);
	}

	public void buildSchedule(){
		System.out.println("Running BuildSchedule");
		class RabbitGrassStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(agentList);
				rgSpace.growGrass(grassGrowth);
				for(int i =0; i < agentList.size(); i++){
					RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
					rga.step();
				}
				for(int i =0; i < agentList.size(); i++){
					RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
					if(rga.getStomach() < 1){
						rgSpace.removeAgentAt(rga.getX(), rga.getY());
						agentList.remove(i);
					}
				}
				for(int i =0; i < agentList.size(); i++){
					RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
					if(rga.getStomach() > 2 * rabbitStomach){
						rga.giveBirth(rabbitStomach);
						addNewAgent();
					}
				}				
				displaySurf.updateDisplay();
                                graph.step();
				try { 
					Thread.sleep(slowDown); 
				}catch(Exception e) {
					System.out.println("Exception caught");
				}
			}
		}

		schedule.scheduleActionBeginning(0, new RabbitGrassStep());

		class RabbitGrassCountLiving extends BasicAction {
			public void execute(){
				countLivingAgents();
			}
		}

		schedule.scheduleActionAtInterval(10, new RabbitGrassCountLiving());
	}

	public void buildDisplay(){
		System.out.println("Running BuildDisplay");

		ColorMap map = new ColorMap();

		for(int i = 1; i<16; i++){
			map.mapColor(i, new Color(0, (int)(i * 8 + 127), 0));
		}
		map.mapColor(0, new Color(71, 56, 41));

		Value2DDisplay displayGrass = 
				new Value2DDisplay(rgSpace.getCurrentGrassSpace(), map);


		Object2DDisplay displayRabbits = new Object2DDisplay(rgSpace.getCurrentAgentSpace());
		displayRabbits.setObjectList(agentList);

		displaySurf.addDisplayableProbeable(displayGrass, "Grass");
		displaySurf.addDisplayableProbeable(displayRabbits, "Rabbits");
	}

	private void addNewAgent(){
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(rabbitStomach);
		boolean available = rgSpace.addAgent(a);
		if (available) agentList.add(a);
	}

	private int countLivingAgents(){
		int livingAgents = 0;
		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)agentList.get(i);
			if(cda.getStomach() > 0) livingAgents++;
		}
		return livingAgents;
	}


	public int getNumAgents(){
		return numAgents;
	}

	public void setNumAgents(int na){
		numAgents = na;
	}

	public int getWorldXSize(){
		return worldXSize;
	}

	public void setWorldXSize(int wxs){
		worldXSize = wxs;
	}

	public int getWorldYSize(){
		return worldYSize;
	}

	public void setWorldYSize(int wys){
		worldYSize = wys;
	}

	public int getRabbitStomach() {
		return rabbitStomach;
	}

	public void setRabbitStomach(int i) {
		rabbitStomach = i;
	}

	public int getGrassGrowth() {
		return grassGrowth;
	}

	public void setGrassGrowth(int i) {
		grassGrowth = i;
	}

	public int getSlowDown() {
		return slowDown;
	}

	public void setSlowDown(int i) {
		slowDown = i;
	}
}