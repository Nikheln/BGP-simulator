package bgp.core.messages.notificationexceptions;

import bgp.core.messages.NotificationMessage;
import bgp.core.messages.NotificationMessage.OpenMessageError;

public class OpenMessageException extends NotificationException {

	private static final long serialVersionUID = 1L;
	private final OpenMessageError error;
	
	public OpenMessageException(OpenMessageError error) {
		super();
		this.error = error;
	}

	@Override
	public NotificationMessage buildNotification() {
		return NotificationMessage.getOpenMessageError(error);
	}

}
