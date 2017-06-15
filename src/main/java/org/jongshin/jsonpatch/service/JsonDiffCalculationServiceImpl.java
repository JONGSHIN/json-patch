package org.jongshin.jsonpatch.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.jongshin.jsonpatch.pojo.DiffNode;
import org.jongshin.jsonpatch.pojo.DiffNode.DiffNodeBuilder;
import org.jongshin.jsonpatch.pojo.HistoryConfig;
import org.jongshin.jsonpatch.pojo.Operation;
import org.jongshin.jsonpatch.util.JsonUtils;

import com.google.common.base.Preconditions;

/**
 * 
 * @author Vitalii_Kim
 *
 */
public class JsonDiffCalculationServiceImpl implements JsonDiffCalculationService {

	private HistoryConfig historyConfig;

	public JsonDiffCalculationServiceImpl() {
		historyConfig = JsonUtils.streamToBean(
				JsonDiffCalculationServiceImpl.class.getResourceAsStream("/history-config.json"), HistoryConfig.class);
	}

	@Override
	public List<DiffNode> generateDiffs(String sourceJson, String targetJson, String entityType) {
		Preconditions.checkNotNull(sourceJson);
		Preconditions.checkNotNull(targetJson);
		Preconditions.checkNotNull(entityType);
		List<Object> path = new ArrayList<>();
		List<DiffNode> diffNodes = new ArrayList<>();
		compareNodes(path, diffNodes, JsonUtils.stringToJsonNode(sourceJson), JsonUtils.stringToJsonNode(targetJson),
				entityType);
		// TODO debug
		diffNodes.stream().forEach(System.out::println);
		return diffNodes;
	}

	private void compareNodes(List<Object> path, List<DiffNode> diffNodes, JsonNode sourceNode, JsonNode targetNode,
			String entityType) {
		Preconditions.checkNotNull(path);
		Preconditions.checkNotNull(diffNodes);
		Preconditions.checkNotNull(sourceNode);
		Preconditions.checkNotNull(targetNode);
		Preconditions.checkNotNull(entityType);
		if (!sourceNode.equals(targetNode)) {
			if (sourceNode.isObject() && targetNode.isObject()) {
				compareObjectNodes(path, diffNodes, (ObjectNode) sourceNode, (ObjectNode) targetNode, entityType);
			} else if (sourceNode.isArray() && targetNode.isArray()) {
				compareArrayNodes(path, diffNodes, (ArrayNode) sourceNode, (ArrayNode) targetNode, entityType);
			} else {
				diffNodes.add(new DiffNodeBuilder().value(targetNode).op(Operation.REPLACE).path(path).build());
			}
		}
	}

	private void compareObjectNodes(List<Object> path, List<DiffNode> diffNodes, ObjectNode sourceNode,
			ObjectNode targetNode, String entityType) {
		Preconditions.checkNotNull(path);
		Preconditions.checkNotNull(diffNodes);
		Preconditions.checkNotNull(sourceNode);
		Preconditions.checkNotNull(targetNode);
		Preconditions.checkNotNull(entityType);
		Iterator<String> sourceFieldNamesIterator = sourceNode.getFieldNames();
		while (sourceFieldNamesIterator.hasNext()) {
			String sourceFieldName = sourceFieldNamesIterator.next();
			if (!targetNode.has(sourceFieldName)) {
				diffNodes.add(new DiffNodeBuilder().value(sourceNode.get(sourceFieldName)).op(Operation.REMOVE)
						.path(constructPath(path, sourceFieldName)).build());
				continue;
			}
			compareNodes(constructPath(path, sourceFieldName), diffNodes, sourceNode.get(sourceFieldName),
					targetNode.get(sourceFieldName), entityType);
		}
		Iterator<String> targetFieldNamesIterator = targetNode.getFieldNames();
		while (targetFieldNamesIterator.hasNext()) {
			String targetFieldName = targetFieldNamesIterator.next();
			if (!sourceNode.has(targetFieldName)) {
				diffNodes.add(new DiffNodeBuilder().value(targetNode.get(targetFieldName)).op(Operation.ADD)
						.path(constructPath(path, targetFieldName)).build());
			}
		}
	}

	private void compareArrayNodes(List<Object> path, List<DiffNode> diffNodes, ArrayNode sourceArray,
			ArrayNode targetArray, String entityType) {
		Preconditions.checkNotNull(path);
		Preconditions.checkNotNull(diffNodes);
		Preconditions.checkNotNull(sourceArray);
		Preconditions.checkNotNull(targetArray);
		Preconditions.checkNotNull(entityType);
		String uid = historyConfig.getUid(entityType, path);
		if (uid != null) {
			List<JsonNode> symmetricDifference = getSymmetricDifference(JsonUtils.jsonArrayToList(sourceArray),
					JsonUtils.jsonArrayToList(targetArray), uid);
			validateArrayNode(sourceArray, uid);
			validateArrayNode(targetArray, uid);
			int symmetricDifferenceIndex = 0;
			while (symmetricDifferenceIndex < symmetricDifference.size()) {
				JsonNode symmetricDifferenceNode = symmetricDifference.get(symmetricDifferenceIndex);
				if (indexOf(JsonUtils.jsonArrayToList(sourceArray), symmetricDifferenceNode, uid) != -1
						&& indexOf(JsonUtils.jsonArrayToList(targetArray), symmetricDifferenceNode, uid) == -1) {
					diffNodes.add(new DiffNodeBuilder().value(symmetricDifferenceNode).op(Operation.REMOVE)
							.path(constructPath(path,
									indexOf(JsonUtils.jsonArrayToList(sourceArray), symmetricDifferenceNode, uid)))
							.build());
					symmetricDifferenceIndex++;
				} else if (indexOf(JsonUtils.jsonArrayToList(targetArray), symmetricDifferenceNode, uid) != -1
						&& indexOf(JsonUtils.jsonArrayToList(sourceArray), symmetricDifferenceNode, uid) == -1) {
					diffNodes.add(new DiffNodeBuilder().value(symmetricDifferenceNode).op(Operation.ADD)
							.path(constructPath(path,
									indexOf(JsonUtils.jsonArrayToList(targetArray), symmetricDifferenceNode, uid)))
							.build());
					symmetricDifferenceIndex++;
				} else {
					int sourceIndex = indexOf(JsonUtils.jsonArrayToList(sourceArray), symmetricDifferenceNode, uid);
					compareNodes(constructPath(path, sourceIndex), diffNodes, sourceArray.get(sourceIndex),
							symmetricDifferenceNode, entityType);
					symmetricDifferenceIndex++;
				}
			}
		} else {
			List<JsonNode> longestCommonSubsequence = getLongestCommonSubsequence(
					JsonUtils.jsonArrayToList(sourceArray), JsonUtils.jsonArrayToList(targetArray));
			int sourceIndex = 0;
			int targetIndex = 0;
			int lcsIndex = 0;
			int lcsSize = longestCommonSubsequence.size();
			int sourceSize = sourceArray.size();
			int targetSize = targetArray.size();
			int position = 0;
			while (lcsIndex < lcsSize) {
				JsonNode lcsNode = longestCommonSubsequence.get(lcsIndex);
				JsonNode sourceNode = sourceArray.get(sourceIndex);
				JsonNode targetNode = targetArray.get(targetIndex);
				if (lcsNode.equals(sourceNode) && lcsNode.equals(targetNode)) {
					sourceIndex++;
					targetIndex++;
					lcsIndex++;
					position++;
				} else {
					if (lcsNode.equals(sourceNode)) {
						diffNodes.add(new DiffNodeBuilder().value(targetNode).op(Operation.ADD)
								.path(constructPath(path, position)).build());
						position++;
						targetIndex++;
					} else if (lcsNode.equals(targetNode)) {
						diffNodes.add(new DiffNodeBuilder().value(sourceNode).op(Operation.REMOVE)
								.path(constructPath(path, sourceIndex)).build());
						sourceIndex++;
					} else {
						compareNodes(constructPath(path, sourceIndex), diffNodes, sourceNode, targetNode, entityType);
						sourceIndex++;
						targetIndex++;
						position++;
					}
				}
			}
			while ((sourceIndex < sourceSize) && (targetIndex < targetSize)) {
				JsonNode sourceNode = sourceArray.get(sourceIndex);
				JsonNode targetNode = targetArray.get(targetIndex);
				compareNodes(constructPath(path, sourceIndex), diffNodes, sourceNode, targetNode, entityType);
				sourceIndex++;
				targetIndex++;
				position++;
			}
			while (targetIndex < targetSize) {
				diffNodes.add(new DiffNodeBuilder().value(targetArray.get(targetIndex)).op(Operation.ADD)
						.path(constructPath(path, position)).build());
				position++;
				targetIndex++;
			}
			while (sourceIndex < sourceSize) {
				diffNodes.add(new DiffNodeBuilder().value(sourceArray.get(sourceIndex)).op(Operation.REMOVE)
						.path(constructPath(path, sourceIndex)).build());
				sourceIndex++;
			}
			toPrettyFormat(diffNodes);
		}
	}

	private void toPrettyFormat(List<DiffNode> diffNodes) {
		Preconditions.checkNotNull(diffNodes);
		for (int i = 0; i < diffNodes.size(); i++) {
			DiffNode diffNode = diffNodes.get(i);
			if (!(Operation.REMOVE == diffNode.getOp() || Operation.ADD == diffNode.getOp())) {
				continue;
			}
			for (int j = i + 1; j < diffNodes.size(); j++) {
				DiffNode temp = diffNodes.get(j);
				if (!diffNode.getValue().equals(temp.getValue())) {
					continue;
				}
				DiffNode moveDiff = null;
				if (Operation.REMOVE == diffNode.getOp() && Operation.ADD == temp.getOp()) {
					moveDiff = new DiffNodeBuilder().value(temp.getValue()).op(Operation.MOVE).path(temp.getPath())
							.build();
				} else if (Operation.ADD == diffNode.getOp() && Operation.REMOVE == temp.getOp()) {
					moveDiff = new DiffNodeBuilder().value(diffNode.getValue()).op(Operation.MOVE)
							.path(diffNode.getPath()).build();
				}
				if (moveDiff != null) {
					diffNodes.remove(j);
					diffNodes.set(i, moveDiff);
					break;
				}
			}
		}
	}

	/**
	 * Produces disjunctive union, of two sets is the set of elements which are
	 * in either of the sets and not in their intersection, using the specified
	 * {@code UID} to compare values
	 * 
	 * @param sourceList
	 * @param targetList
	 * @param uid
	 *            the field used for comparison
	 * @return list of {@linkplain JsonNode}, empty list otherwise
	 * @throws NullPointerException
	 *             if at least one of the following parameters:
	 *             {@code sourceList}, {@code targetList} has null value
	 */
	private List<JsonNode> getSymmetricDifference(List<JsonNode> sourceList, List<JsonNode> targetList, String uid) {
		Preconditions.checkNotNull(sourceList);
		Preconditions.checkNotNull(targetList);
		if (uid == null) {
			return new ArrayList<>();
		}
		List<JsonNode> symmetricDifference = new ArrayList<>(sourceList);
		int idx = -1;
		int position = 0;
		for (JsonNode targetNode : targetList) {
			if ((idx = indexOf(symmetricDifference, targetNode, uid)) != -1) {
				if (!symmetricDifference.get(idx).equals(targetNode)) {
					symmetricDifference.remove(idx);
					symmetricDifference.add(idx, targetNode);
					position++;
				} else {
					symmetricDifference.remove(idx);
				}
			} else {
				symmetricDifference.add(position, targetNode);
				position++;
			}
		}
		return Collections.unmodifiableList(symmetricDifference);
	}

	/**
	 * Returns the index within specified list of the first occurrence of the
	 * specified element, using the specified {@code UID} to compare values
	 * 
	 * @param jsonNodes
	 *            the list where to search for
	 * @param jsonNode
	 *            element whose presence in this list is to be tested
	 * @param uid
	 *            the field used for comparison
	 * @return the index of the first occurrence of the specified element, or
	 *         {@code -1} if there is no such occurrence.
	 */
	private int indexOf(List<JsonNode> jsonNodes, JsonNode jsonNode, String uid) {
		Preconditions.checkNotNull(jsonNodes);
		if (jsonNode == null || uid == null) {
			return -1;
		}
		for (int i = 0; i < jsonNodes.size(); i++) {
			if (hasEqualUid(jsonNodes.get(i), jsonNode, uid)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Ensures that specified array contains only one element with the given
	 * {@code UID}
	 * 
	 * @param arrayNode
	 *            array to be validated
	 * @param uid
	 *            the field used for comparison
	 */
	private void validateArrayNode(ArrayNode arrayNode, String uid) {
		Preconditions.checkNotNull(arrayNode);
		if (uid == null) {
			return;
		}
		for (int i = 0; i < arrayNode.size(); i++) {
			JsonNode jsonNode = arrayNode.get(i);
			for (int j = i + 1; j < arrayNode.size(); j++) {
				JsonNode temp = arrayNode.get(j);
				if (hasEqualUid(jsonNode, temp, uid)) {
					throw new IllegalStateException(
							String.format("Integrity constraint violation: Field %s is ambiguous", uid));
				}
			}
		}
	}

	private boolean hasEqualUid(JsonNode sourceNode, JsonNode targetNode, String uid) {
		Preconditions.checkNotNull(sourceNode);
		Preconditions.checkNotNull(targetNode);
		return sourceNode.has(uid) && sourceNode.get(uid).equals(targetNode.get(uid));
	}

	/**
	 * Returns the longest subsequence common to all sequences in a set of two
	 * sequences
	 * 
	 * @param sourceList
	 * @param targetList
	 * @return list of {@link} JsonNode, empty list otherwise
	 */
	private List<JsonNode> getLongestCommonSubsequence(List<JsonNode> sourceList, List<JsonNode> targetList) {
		Preconditions.checkNotNull(sourceList);
		Preconditions.checkNotNull(targetList);
		int sourceSize = sourceList.size();
		int targetSize = targetList.size();
		if (sourceSize == 0 || targetSize == 0) {
			return new ArrayList<>();
		} else if (sourceList.get(sourceSize - 1).equals(targetList.get(targetSize - 1))) {
			List<JsonNode> accumulator = getLongestCommonSubsequence(sourceList.subList(0, sourceSize - 1),
					targetList.subList(0, targetSize - 1));
			accumulator.add(sourceList.get(sourceSize - 1));
			return accumulator;
		} else {
			List<JsonNode> sourceIdentity = getLongestCommonSubsequence(sourceList,
					targetList.subList(0, targetSize - 1));
			List<JsonNode> targetIdentity = getLongestCommonSubsequence(sourceList.subList(0, sourceSize - 1),
					targetList);
			if (sourceIdentity.size() > targetIdentity.size()) {
				return sourceIdentity;
			}
			return targetIdentity;
		}
	}

	private List<Object> constructPath(List<Object> parentPath, Object... childPath) {
		Preconditions.checkNotNull(parentPath);
		if (childPath.length == 0) {
			return Collections.unmodifiableList(parentPath);
		}
		List<Object> result = new ArrayList<>(parentPath);
		result.addAll(Arrays.asList(childPath));
		return Collections.unmodifiableList(result);
	}
}
