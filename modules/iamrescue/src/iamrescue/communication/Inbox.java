package iamrescue.communication;

import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.filter.IsReadMessagePredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javolution.util.FastList;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.functors.AndPredicate;
import org.apache.commons.collections15.functors.NotPredicate;
import org.apache.log4j.Logger;

/**
 * A class that is responsible for containing received messages, without any
 * intelligence.
 * 
 * @author Ruben Stranders, Alessandro Farinelli
 * 
 */
public class Inbox {

	private static Predicate<Message> notReadPredicate = NotPredicate
			.getInstance(new IsReadMessagePredicate());

	private static final Logger LOGGER = Logger.getLogger(Inbox.class);

	/**
	 * List received messages
	 */
	private final List<Message> inbox = new ArrayList<Message>();

	/**
	 * Updates the inbox with new messages
	 * 
	 */
	public void updateInbox() {
	}

	public void addMessage(Message msg) {
		inbox.add(msg);
	}

	public Iterator<Message> getUnreadMessagesIterator() {
		return getUnreadMessages().iterator();
	}

	public Iterator<Message> getAllMessagesIterator() {
		return inbox.iterator();
	}

	public List<Message> getAllMessages() {
		return inbox;
	}

	public Collection<Message> getUnreadMessages() {
		List<Message> unread = new FastList<Message>();
		for (Message message : inbox) {
			if (!message.isRead()) {
				unread.add(message);
			}
		}
		return unread;
	}

	public void addMessages(List<Message> messages) {
		inbox.addAll(messages);

	}

	public Collection<Message> getUnreadMessages(IMessageFilter messageFilter) {
		return CollectionUtils.select(inbox, AndPredicate.getInstance(
				notReadPredicate, messageFilter));
	}

	public void removeReadMessages() {
		Iterator<Message> iterator = inbox.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().isRead()) {
				iterator.remove();
			}
		}
	}
}
