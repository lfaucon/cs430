package template;

import logist.task.Task;

public class Action {
	Task pickup;
	Task deliver;
	
	public Action(Task pickup, Task deliver) {
		this.pickup = pickup;
		this.deliver = deliver;
	}
}
