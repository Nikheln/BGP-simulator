package bgp.core.messages.notificationexceptions;

import bgp.core.messages.NotificationMessage;
import bgp.core.messages.NotificationMessage.MessageHeaderError;

public class MessageHeaderException extends NotificationException {

	private static final long serialVersionUID = 1L;
	private final MessageHeaderError error;
	
	public MessageHeaderException(MessageHeaderError error) {
		super();
		this.error = error;
	}

	@Override
	public NotificationMessage buildNotification() {
		return NotificationMessage.getMessageHeaderError(error);
	}

}
