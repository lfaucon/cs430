package template;

import java.awt.Color;

import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class VehicleBis implements Vehicle {

	private int capacity;
	private int costPerKm;
	private City currentCity;
	
	VehicleBis(int capacity, int costPerKm, City currentCity){
		this.capacity = capacity;
		this.costPerKm = costPerKm;
		this.currentCity = currentCity;
	}
	
	public int capacity() {
		return capacity;
	}
	
	public int costPerKm() {
		return costPerKm;
	}
	
	public City getCurrentCity() {
		return currentCity;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public City homeCity() {
		// TODO Auto-generated method stub
		return currentCity;
	}

	@Override
	public double speed() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getReward() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDistanceUnits() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDistance() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Color color() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int id() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public TaskSet getCurrentTasks() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
