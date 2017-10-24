package template;

import java.util.HashSet;

import logist.task.Task;
import logist.topology.Topology.City;

public class State implements Comparable<Object>{
	public City currentCity;
	public HashSet<Task> restTasks; //all remaining tasks
	public HashSet<Task> currentTasks; // all picked tasks
	public Double cost, g;
	public Double weight;
	
	public State(City currentCity) {
		this.currentCity = currentCity;
		this.restTasks = new HashSet<Task>();
		this.currentTasks = new HashSet<Task>();
		this.g = 0.;
		this.cost = 0.;
		this.weight = 0.;
	}
	
	@Override
	public int compareTo(Object otherState) {
		if(this.cost>((State) otherState).cost) {
			return 1;
		} else {
			return -1;
		}
	}
	
	@Override
	public boolean equals(Object otherState) {
		boolean a = restTasks.containsAll(((State) otherState).restTasks);
		boolean b = restTasks.containsAll(((State) otherState).currentTasks);
		boolean c = ((State) otherState).restTasks.containsAll(restTasks);
		boolean d = ((State) otherState).restTasks.containsAll(restTasks);
		return(a && b && c && d);
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 17 + restTasks.hashCode();
		hash = hash * 31 + currentTasks.hashCode();
		return(hash);
	}
}