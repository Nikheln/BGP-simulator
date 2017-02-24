package bgp.core.messages.notificationexceptions;

import bgp.core.messages.NotificationMessage;
import bgp.core.messages.NotificationMessage.UpdateMessageError;

public class UpdateMessageException extends NotificationException {

	private static final long serialVersionUID = 1L;
	private final UpdateMessageError error;
	
	public UpdateMessageException(UpdateMessageError error) {
		super();
		this.error = error;
	}
	
	@Override
	public NotificationMessage buildNotification() {
		return NotificationMessage.getUpdateMessageError(error);
	}
}
