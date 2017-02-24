package bgp.core.messages.notificationexceptions;

import bgp.core.messages.NotificationMessage;

/**
 * Subclass for exceptions that require a NOTIFICATION
 * message to be sent.
 * 
 * @author Niko
 *
 */
public abstract class NotificationException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public NotificationException() {
		super();
	}
	
	public abstract NotificationMessage buildNotification();
}
