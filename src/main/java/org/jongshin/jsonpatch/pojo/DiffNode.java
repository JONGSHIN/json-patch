package org.jongshin.jsonpatch.pojo;

import java.io.Serializable;
import java.util.List;

import org.codehaus.jackson.JsonNode;

/**
 * 
 * @author Vitalii_Kim
 *
 */
public class DiffNode implements Serializable {

	private static final long serialVersionUID = -3021800402129247515L;

	private final Operation op;
	private final List<Object> path;
	private final JsonNode value;

	public DiffNode(DiffNodeBuilder builder) {
		this.op = builder.op;
		this.path = builder.path;
		this.value = builder.value;
	}

	public Operation getOp() {
		return op;
	}

	public List<Object> getPath() {
		return path;
	}

	public JsonNode getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "DiffNode [op=" + op + ", path=" + path + ", value=" + value + "]";
	}

	public static class DiffNodeBuilder {
		private Operation op;
		private List<Object> path;
		private JsonNode value;

		public DiffNodeBuilder op(Operation op) {
			this.op = op;
			return this;
		}

		public DiffNodeBuilder path(List<Object> path) {
			this.path = path;
			return this;
		}

		public DiffNodeBuilder value(JsonNode value) {
			this.value = value;
			return this;
		}

		public DiffNode build() {
			if (op == null || path == null) {
				throw new IllegalStateException();
			}
			return new DiffNode(this);
		}
	}
}
