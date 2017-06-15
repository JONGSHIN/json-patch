package org.jongshin.jsonpatch.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.base.Preconditions;

/**
 * 
 * @author Vitalii_Kim
 *
 */
public class JsonUtils {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final ObjectReader READER;
	private static final ObjectWriter WRITER;

	static {
		MAPPER.setSerializationConfig(
				MAPPER.getSerializationConfig().withSerializationInclusion(JsonSerialize.Inclusion.ALWAYS));
		MAPPER.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		MAPPER.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
		MAPPER.configure(SerializationConfig.Feature.REQUIRE_SETTERS_FOR_GETTERS, true);
		READER = MAPPER.reader();
		WRITER = MAPPER.writer();
	}

	public static <T> T stringToBean(String string, Class<T> toType) {
		Preconditions.checkNotNull(string);
		Preconditions.checkNotNull(toType);
		try {
			return READER.withType(toType).readValue(string);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Map<String, Object> stringToMap(String string) {
		Preconditions.checkNotNull(string);
		try {
			return READER.withType(Map.class).readValue(string);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T streamToBean(InputStream is, Class<T> toType) {
		Preconditions.checkNotNull(is);
		Preconditions.checkNotNull(toType);
		try {
			return READER.withType(toType).readValue(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Map<String, Object> streamToMap(InputStream is) {
		Preconditions.checkNotNull(is);
		try {
			return READER.withType(Map.class).readValue(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String streamToString(InputStream is) {
		Preconditions.checkNotNull(is);
		try {
			return jsonNodeToString(READER.readTree(is));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String beanToString(Object bean) {
		Preconditions.checkNotNull(bean);
		try {
			return WRITER.writeValueAsString(bean);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static JsonNode stringToJsonNode(String string) {
		Preconditions.checkNotNull(string);
		try {
			return MAPPER.readTree(string);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String jsonNodeToString(JsonNode jsonNode) {
		Preconditions.checkNotNull(jsonNode);
		if (jsonNode.isObject() || jsonNode.isArray()) {
			return jsonNode.toString();
		} else if (jsonNode.isNull()) {
			return null;
		} else {
			return jsonNode.asText();
		}
	}

	public static List<JsonNode> jsonArrayToList(ArrayNode arrayNode) {
		Preconditions.checkNotNull(arrayNode);
		return StreamSupport.stream(arrayNode.spliterator(), false).collect(Collectors.toList());
	}

	public static Map<String, JsonNode> jsonObjectToMap(ObjectNode objectNode) {
		Preconditions.checkNotNull(objectNode);
		Map<String, JsonNode> result = new LinkedHashMap<>();
		Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.getFields();
		while (iterator.hasNext()) {
			result.put(iterator.next().getKey(), iterator.next().getValue());
		}
		return result;
	}

	/**
	 * Returns a {@code JsonNode} in specified JSON with the given path. If no
	 * such element exists, this returns {@code null}
	 * 
	 * @param json
	 *            the JSON where to search for
	 * @param path
	 *            path to JSON node in tree
	 * @return {@code JsonNode}, {@code null} otherwise
	 */
	public static JsonNode getNode(String json, List<Object> path) {
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(path);
		return getNode(stringToJsonNode(json), path, path.size() - 1, 0);
	}

	public static JsonNode getNode(JsonNode jsonNode, List<Object> path) {
		Preconditions.checkNotNull(jsonNode);
		Preconditions.checkNotNull(path);
		return getNode(jsonNode, path, path.size() - 1, 0);
	}

	/**
	 * Returns a {@code JsonNode} in specified JSON with the given path. If no
	 * such element exists, this returns {@code null}
	 * 
	 * @param jsonNode
	 *            the JSON where to search for
	 * @param path
	 *            path to JSON node in tree
	 * @param position
	 *            the ending index, exclusive.
	 * @param step
	 *            the beginning index, inclusive.
	 * @return
	 */
	public static JsonNode getNode(JsonNode jsonNode, List<Object> path, int position, int step) {
		Preconditions.checkNotNull(jsonNode);
		Preconditions.checkNotNull(path);
		if (position >= path.size() || position < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (step > position) {
			return jsonNode;
		}
		String key = String.valueOf(path.get(step));
		if (jsonNode.isArray()) {
			return getNode(jsonNode.get(Integer.parseInt(key)), path, position, ++step);
		} else if (jsonNode.isObject()) {
			if (jsonNode.has(key)) {
				return getNode(jsonNode.get(key), path, position, ++step);
			}
			return null;
		} else {
			return jsonNode;
		}
	}
}
