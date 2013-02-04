package com.nhn.android.m2base.object;

import com.nhn.android.m2base.util.internal.M2baseUtility;


@SuppressWarnings("serial")
public class ApiResponse extends BaseObj {
	private static final String CODE = "code";
	private static final String MESSAGE = "message";
	private static final String DESCRIPTION = "description";
	private static final String LOCALIZED_MESSAGE = "localizedMessage";
	private static final String LOCALIZED_DESCRIPTION = "localizedDescription";
	
	public String getCode() {
		return getString(CODE);
	}
	
	public void setCode(String code) {
		put(CODE, code);
	}
	
	public String getMessage() {
		String localizedMessage = getLocalizedMessage();
		if (M2baseUtility.isNotNullOrEmpty(localizedMessage)) {
			return localizedMessage;
		}
		
		return getString(MESSAGE, "");
	}
	
	public void setMessage(String message) {
		put(MESSAGE, message);
	}
	
	public String getDescription() {
		String localizedDescription = getLocalizedDescription();
		if (M2baseUtility.isNotNullOrEmpty(localizedDescription)) {
			return localizedDescription;
		}
		
		return getString(DESCRIPTION, "");
	}
	
	public void setDescription(String description) {
		put(DESCRIPTION, description);
	}
	
	public String getLocalizedMessage() {
		return getString(LOCALIZED_MESSAGE, "");
	}
	
	public void setLocalizedMessage(String localizedMessage) {
		put(LOCALIZED_MESSAGE, localizedMessage);
	}
	
	public String getLocalizedDescription() {
		return getString(LOCALIZED_DESCRIPTION, "");
	}
	
	public void setLocalizedDescription(String localizedDescription) {
		put(LOCALIZED_DESCRIPTION, localizedDescription);
	}
}
