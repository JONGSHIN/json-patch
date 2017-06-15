package org.jongshin.jsonpatch.service;

import java.util.List;

import org.jongshin.jsonpatch.pojo.DiffNode;

/**
 * 
 * @author Vitalii_Kim
 *
 */
public interface JsonDiffCalculationService {

	/**
	 * Finds JSON Patches according to RFC 6902. JSON Patch defines a JSON
	 * document structure for representing changes to a JSON document. Method
	 * computes and returns a JSON Patch from source to target, both source and
	 * target must be either valid JSON objects or arrays or values. The
	 * algorithm which computes this JsonPatch currently generates following
	 * operations as per RFC 6902 -
	 *
	 * ADD, REMOVE, REPLACE, MOVE
	 * 
	 * @param sourceJson
	 *            first JSON to compare
	 * @param targetJson
	 *            second JSON to compare
	 * @param entityType
	 *            type of comparable entity
	 * @return list of {@link DiffNode}, empty list if there are no changes
	 * @throws NullPointerException
	 *             if at least one parameter has null value
	 */
	List<DiffNode> generateDiffs(String sourceJson, String targetJson, String entityType);
}
