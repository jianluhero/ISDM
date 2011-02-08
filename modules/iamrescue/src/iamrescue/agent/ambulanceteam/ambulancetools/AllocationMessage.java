package iamrescue.agent.ambulanceteam.ambulancetools;

import java.util.Arrays;

import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.codec.IMessageCodec;

public class AllocationMessage extends Message {

	private int[] task;
	private int time;

	public AllocationMessage(int[] nextTasksID, int time2) {
		// TODO Auto-generated constructor stub
		this.task = nextTasksID;
		this.time = time2;
	}

	public String toShortString() {
		return getClass().getSimpleName() + "[s:" + getSenderAgentID() + ",t:"
				+ time + "]";
	}

	public void setTask(int[] task) {
		this.task = task;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public int[] getTask() {
		return task;
	}

	public int getTime() {
		return time;
	}

	@Override
	public Message copy() {
		// TODO Auto-generated method stub
		return new AllocationMessage(task, time);
	}

	@Override
	public IMessageCodec<AllocationMessage> getCodec() {
		// TODO Auto-generated method stub
		return new AllocationCodec();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(task);
		result = prime * result + time;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AllocationMessage other = (AllocationMessage) obj;
		if (!Arrays.equals(task, other.task))
			return false;
		if (time != other.time)
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.Message#getMessageContentsAsString()
	 */
	@Override
	public String getMessageContentsAsString() {
		StringBuffer sb = new StringBuffer();
		sb.append("time=");
		sb.append(time);
		sb.append(",task=");
		sb.append(Arrays.toString(task));
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.communication.messages.Message#getMessageName()
	 */
	@Override
	public String getMessageName() {
		return "AllocationMessage";
	}

}
