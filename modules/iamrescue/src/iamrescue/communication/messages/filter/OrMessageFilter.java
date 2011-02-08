package iamrescue.communication.messages.filter;

import iamrescue.communication.messages.Message;

import java.util.Collection;


/**
 * This filter will iterate through a collection of filters. As soon as one
 * filter accepts the message, it is assumed accepted by the parent filter
 * (regardless of whether other filters reject the message). Not all filters are
 * necessarily called when one has accepted.
 * 
 * @author ss2
 * 
 */
public class OrMessageFilter implements IMessageFilter {

	private Collection<IMessageFilter> filters;

	public OrMessageFilter(Collection<IMessageFilter> filters) {
		this.filters = filters;
	}

	public boolean evaluate(Message message) {
		for (IMessageFilter filter : filters) {
			if (filter.evaluate(message)) {
				return true;
			}
		}
		return false;
	}
}
