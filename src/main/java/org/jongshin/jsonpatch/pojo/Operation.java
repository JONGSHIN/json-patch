package org.jongshin.jsonpatch.pojo;

import java.util.Arrays;

/**
 * 
 * @author Vitalii_Kim
 *
 */
public enum Operation {
	ADD("add"), REMOVE("remove"), REPLACE("replace"), MOVE("move");

	private String rfcName;

	private Operation(String rfcName) {
		this.rfcName = rfcName;
	}

	public Operation getByName(String rfcName) {
		return Arrays.stream(values()).filter(rfcName::equals).findFirst().orElse(null);
	}

	public String getRfcName() {
		return rfcName;
	}

}
