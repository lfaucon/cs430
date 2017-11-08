package template;

import java.util.List;
import java.util.Random;

public class LocalChoice {

	static Solution get(List<Solution> ls, double p){

		// With probability p we pick a solution at random
		Random rand = new Random();
		if (rand.nextDouble() < p) return ls.get(rand.nextInt(ls.size()));

		// With probability 1-p we pick the best solution
		Solution sol = ls.get(0);
		for(Solution s: ls){
			if(s.cost < sol.cost){
				sol = s;
			}
		}
		return sol;
	}

}
