import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int stomach;
	private int stomachSize;
	private int ID;
	private RabbitsGrassSimulationSpace rgSpace;

	public RabbitsGrassSimulationAgent(int rabbitStomach){
		x = -1;
		y = -1;
		stomachSize = rabbitStomach;
		stomach = (int)((Math.random() * rabbitStomach + rabbitStomach));
	}

	public void setXY(int newX, int newY){
		x = newX;
		y = newY;
	}

	public void setRabbitGrassSpace(RabbitsGrassSimulationSpace rgs){
		rgSpace = rgs;
	}

	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}

	public String getID(){
		return "A-" + ID;
	}

	public int getStomach(){
		return stomach;
	}

	public void report(){
		String rep = getID() + " at " + 
				x + ", " + y + 
				" has " + 
				getStomach() + " grass in the stomach.";
		//System.out.println(rep);
	}
	
	public void giveBirth(int energy) {
		stomach -= energy;
	}

	public void step(){
		int vX = 0, vY = 0;
		if(Math.random() > 0.5)
			if(Math.random() > 0.5)
				vX = 1;
			else
				vX = -1;
		else 
			if(Math.random() > 0.5)
				vY = 1;
			else
				vY = -1;

		int newX = x + vX, newY = y + vY;

		Object2DGrid grid = rgSpace.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		if(tryMove(newX, newY)){
			stomach += rgSpace.eatGrassAt(x, y);
		}
		stomach--;
	}

	private boolean tryMove(int newX, int newY){
		return rgSpace.moveAgentAt(x, y, newX, newY);
	}

	public void draw(SimGraphics G) {
		// TODO Auto-generated method stub
		if(stomach > stomachSize / 2)
			G.drawFastRoundRect(Color.white);
		else
			G.drawFastRoundRect(Color.lightGray);

	}
}
