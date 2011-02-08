package iamrescue.communication;

import iamrescue.communication.messages.Message;

import org.apache.commons.collections15.Predicate;


public interface IMessageFilter extends Predicate<Message> {

}
