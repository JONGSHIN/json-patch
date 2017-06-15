package org.jongshin.jsonpatch;

import org.jongshin.jsonpatch.service.JsonDiffCalculationServiceImpl;
import org.jongshin.jsonpatch.util.JsonUtils;

/**
 * 
 * @author Vitalii_Kim
 *
 */
public class App {
	public static void main(String[] args) {
		new JsonDiffCalculationServiceImpl().generateDiffs(
				JsonUtils.streamToString(App.class.getResourceAsStream("/dfu1.json")),
				JsonUtils.streamToString(App.class.getResourceAsStream("/dfu2.json")),
				"com.epam.e3s.app.dfu.api.data.DfuEntity");
	}
}
