package bgp.core.messages;

import java.util.Arrays;

public class NotificationMessage extends BGPMessage {
	
	protected NotificationMessage(byte[] messageContent) {
		int index = HEADER_LENGTH;
		code = messageContent[index++]&0xFF;
		subcode = messageContent[index++]&0xFF;
		data = Arrays.copyOfRange(messageContent, index, messageContent.length);
	}
	
	private final int code;
	private final int subcode;
	private final byte[] data;
	
	private NotificationMessage(int code, int subcode) {
		this.code = code;
		this.subcode = subcode;
		this.data = new byte[0];
	}

	@Override
	protected byte getType() {
		return (byte) 3;
	}

	@Override
	protected byte[] getBody() {
		byte[] body = new byte[2+data.length];
		int index = 0;
		body[index++] = (byte) (code&0xFF);
		body[index++] = (byte) (subcode&0xFF);
		for (byte b : data) {
			body[index++] = b;
		}
		
		return body;
	}
	
	public ErrorCode getErrorType() {
		return ErrorCode.solveType(code);
	}
	
	public enum ErrorCode {
		MESSAGE_HEADER_ERROR(1),
		OPEN_MESSAGE_ERROR(2),
		UPDATE_MESSAGE_ERROR(3),
		HOLD_TIMER_EXPIRED(4),
		FINITE_STATE_MACHINE_ERROR(5),
		CEASE(6);
		
		private final byte code;
		
		private ErrorCode(int code) {
			this.code = (byte) code;
		}
		
		private static ErrorCode solveType(int code) {
			for (ErrorCode c : ErrorCode.values()) {
				if (c.code == code) {
					return c;
				}
			}
			return CEASE;
		}
	}
	
	public enum MessageHeaderError {
		CONN_NOT_SYNCHRONIZED(1),
		BAD_MESSAGE_LENGTH(2),
		BAD_MESSAGE_TYPE(3);
		
		private final byte subcode;
		
		private MessageHeaderError(int subcode) {
			this.subcode = (byte) subcode;
		}
	}
	
	public enum OpenMessageError {
		UNSUPPORTED_VERSION_NUM(1),
		BAD_PEER_AS(2),
		BAD_BGP_ID(3),
		UNSUPPORTED_OPTIONAL_PARAM(4),
		UNACCEPTABLE_HOLD_TIME(6);
		
		private final byte subcode;
		
		private OpenMessageError(int subcode) {
			this.subcode = (byte) subcode;
		}
	}
	
	public enum UpdateMessageError {
		MALFORMED_ATTRIBUTE_LIST(1),
		UNRECOGNIZED_WELL_KNOWN_ATTRIBUTE(2),
		MISSING_WELL_KNOWN_ATTRIBUTE(3),
		ATTRIBUTE_FLAGS_ERROR(4),
		ATTRIBUTE_LENGTH_ERROR(5),
		INVALID_ORIGIN_ATTRIBUTE(6),
		INVALID_NEXT_HOP_ATTRIBUTE(8),
		OPTIONAL_ATTRIBUTE_ERROR(9),
		INVALID_NETWORK_FIELD(10),
		MALFORMED_AS_PATH(11);
		
		private final byte subcode;
		
		private UpdateMessageError(int subcode) {
			this.subcode = (byte) subcode;
		}
	}
	
	public static NotificationMessage getMessageHeaderError(MessageHeaderError e) {
		return new NotificationMessage(ErrorCode.MESSAGE_HEADER_ERROR.code, e.subcode);
	}
	
	public static NotificationMessage getOpenMessageError(OpenMessageError e) {
		return new NotificationMessage(ErrorCode.OPEN_MESSAGE_ERROR.code, e.subcode);
	}
	
	public static NotificationMessage getUpdateMessageError(UpdateMessageError e) {
		return new NotificationMessage(ErrorCode.UPDATE_MESSAGE_ERROR.code, e.subcode);
	}
	
	public static NotificationMessage getHoldTimeExpiredError() {
		return new NotificationMessage(ErrorCode.HOLD_TIMER_EXPIRED.code, 0);
	}
	
	public static NotificationMessage getFiniteStateMachineError() {
		return new NotificationMessage(ErrorCode.FINITE_STATE_MACHINE_ERROR.code, 0);
	}
	
	public static NotificationMessage getCeaseError() {
		return new NotificationMessage(ErrorCode.CEASE.code, 0);
	}

}
