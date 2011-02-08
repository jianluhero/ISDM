package iamrescue.communication.messages;

import org.apache.commons.collections15.Closure;

public class DecreaseTTLByOne implements Closure<Message> {

	private static DecreaseTTLByOne instance;

	public void execute(Message input) {
		input.setTTL(input.getTTL() - 1);
	}

	public static DecreaseTTLByOne getInstance() {
		if (instance == null) {
			instance = new DecreaseTTLByOne();
		}

		return instance;
	}

}
