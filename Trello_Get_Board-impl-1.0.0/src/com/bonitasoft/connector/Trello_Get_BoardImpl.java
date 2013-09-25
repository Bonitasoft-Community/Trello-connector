/**
 * Copyright (C) 2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * @author Pablo Alonso de Linaje García
 */
package com.bonitasoft.connector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * The connector execution will follow the steps 1 - setInputParameters() -->
 * the connector receives input parameters values 2 - validateInputParameters()
 * --> the connector can validate input parameters values 3 - connect() --> the
 * connector can establish a connection to a remote server (if necessary) 4 -
 * executeBusinessLogic() --> execute the connector 5 - getOutputParameters()
 * --> output are retrieved from connector 6 - disconnect() --> the connector
 * can close connection to remote server (if any)
 */
public class Trello_Get_BoardImpl extends AbstractTrello_Get_BoardImpl {

	private final String CSV_SEPARATOR = ";";
	private final String GET_TRELLO_BOARD_URL = "https://api.trello.com/1/boards/%s?lists=open&list_fields=name,desc&cards=open&fields=name,desc,labelNames&key=%s&token=%s";
	private final List<String> TITLES = Arrays.asList("yellow", "red", "purple", "green", "orange", "blue", "name", "desc", "due", "list", "shortUrl");
	private final List<String> JSON_NAMES = Arrays.asList("labelNames","lists","cards","name","desc","due","list","idList","shortUrl","labels","color");
	private static Logger logger = Logger.getLogger(Trello_Get_BoardImpl.class.getName());
	private Map<String, String> lists;
	private JSONObject labelNames;
	

	@Override
	protected void executeBusinessLogic() throws ConnectorException {
		// Get access to the connector input parameters
		// getBoard();
		// getToken();
		// getApiKey();
		HttpMethod method = null;
		try {
			HttpClient client = new HttpClient();						
			method = new GetMethod(
					String.format(GET_TRELLO_BOARD_URL,
					getBoard(), 
					getApiKey(), 
					getToken()));
			method.addRequestHeader(new Header("Content-Type",
					"application/json; charset=UTF-8"));
			client.executeMethod(method);
			JSONArray array = getJSONFromResponse(method);
			

			List<Map<String, Object>> result = generateBaseStructure(array);
			setTrelloList(result);
			setBonitaColumn(TITLES);
			setBonitaList(getBonitaTableList(result));
			setStringCSV(generateCSVFromList(result));
		} catch (Exception e) {
			throw new ConnectorException(e.getMessage());
		} finally {
			try{
				method.releaseConnection();
			}catch (Exception e) {
				logger.severe("There is a problem releasing Trello Connection");
			}
		}
	}
	

	@Override
	public void validateInputParameters() throws ConnectorValidationException {		
		// TODO Method generated automatically
		super.validateInputParameters();
		
	}

	/**
	 * This method extract data from a specific Trello JSONArray. 
	 * @param array The JSONArray from Trello
	 * @return A list of maps with data from JSON converted  
	 * @throws Exception
	 */
	private List<Map<String, Object>> generateBaseStructure(JSONArray array)
			throws Exception {

		Map<String, Object> map;
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		JSONObject obj = (JSONObject) array.get(0);		
		labelNames = obj.getJSONObject(JSON_NAMES.get(0));
		lists = listToMap(obj.getJSONArray(JSON_NAMES.get(1)));
		JSONArray cards = obj.getJSONArray(JSON_NAMES.get(2));
		JSONArray labels;
		JSONObject card;

		for (int i = 0; i < cards.length(); i++) {

			card = cards.getJSONObject(i);
			map = new HashMap<String, Object>();
			map.put(JSON_NAMES.get(3), card.getString(JSON_NAMES.get(3)));
			map.put(JSON_NAMES.get(4), getDescFormated(card.getString(JSON_NAMES.get(4))));
			
			try {
				map.put(JSON_NAMES.get(5), card.getString(JSON_NAMES.get(5)));
			} catch (Exception e) {
				map.put(JSON_NAMES.get(5), "");
			}
			
			map.put(JSON_NAMES.get(6), lists.get(card.getString(JSON_NAMES.get(7))));
			map.put(JSON_NAMES.get(8), card.getString(JSON_NAMES.get(8)));

			labels = card.getJSONArray(JSON_NAMES.get(9));
			String color;
			for (int j = 0; j < labels.length(); j++) {
				color = labels.getJSONObject(j).getString(JSON_NAMES.get(10));
				map.put(color, labelNames.getString(color));
			}

			list.add(map);

		}
		return list;
	}

	/**
	 * This method replace page breaks into description to white spaces. 
	 * @param description String to be formated
	 * @return String formated
	 */
	private String getDescFormated(String description) {

		return description.replaceAll("(\\r|\\n)", " ");
	}

	/**
	 * Convert a JSONArray into a map.
	 * @param array The JSONArray to convert
	 * @return The map with all the elements of the JSONArray
	 * @throws JSONException
	 */
	private Map<String, String> listToMap(JSONArray array)
			throws JSONException {
		
		Map<String, String> map = new HashMap<String, String>(array.length());
		JSONObject tObj;

		for (int i = 0; i < array.length(); i++) {
			tObj = array.getJSONObject(i);
			map.put(tObj.getString("id"), tObj.getString("name"));
		}
		return map;

	}

	/**
	 * Method to get the specific Trello JSONArray from a http request.
	 * @param method The httpMethod with the response
	 * @return Specific Trello JSONArray
	 * @throws Exception
	 */
	private JSONArray getJSONFromResponse(HttpMethod method) throws Exception {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(method.getResponseBodyAsStream())
				);
		StringBuilder builder = new StringBuilder();

		for (String line = null; (line = reader.readLine()) != null;) {
			builder.append(line).append("\n");
		}
		
		JSONTokener tokener = new JSONTokener(builder.toString());
		JSONObject tObject = null;
		JSONArray tArray = null;
		try {
			tObject = new JSONObject(tokener);
			tArray = new JSONArray();
			tArray.put(tObject);			
		} catch (JSONException e) {
			tokener = new JSONTokener(builder.toString());
			tArray = new JSONArray(tokener);
		}
		return tArray;
	}

	/**
	 * Convert a JSONArray into a list of maps.
	 * @param tArray The JSONArray to convert
	 * @return The list of maps with all the elements of the JSONArray
	 * @throws ConnectorException
	 */
	private List<Map<String, Object>> jsonArrayTOList(JSONArray tArray) throws ConnectorException{
		
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(tArray.length());
		JSONObject tObject = null;

		for (int i = 0; i < tArray.length(); i++) {
			try {
				tObject = tArray.getJSONObject(i);
				list.add(jsonObjectTOMap(tObject));
			} catch (Exception e) {
				throw new ConnectorException("Bad structure : " + e.getMessage());
			}
		}
		return list;
	}

	/**
	 * Convert a JSONObject into a map.
	 * @param obj The JSONObject to convert
	 * @return The map with all the elements of the JSONObject
	 * @throws ConnectorException
	 */
	private Map<String, Object> jsonObjectTOMap(JSONObject obj) throws ConnectorException{
		String[] names = JSONObject.getNames(obj);
		Map<String, Object> map = new HashMap<String, Object>(names.length);
		JSONArray tArray = null;
		JSONObject tObject;
		try {
			for (int i = 0; i < names.length; i++) {
				try {
					tArray = obj.getJSONArray(names[i]);
					map.put(names[i], jsonArrayTOList(tArray));
				} catch (Exception e) {
					try {
						tObject = obj.getJSONObject(names[i]);
						map.put(names[i], jsonObjectTOMap(tObject));
					} catch (Exception e2) {
						map.put(names[i], obj.get(names[i]));
					}

				}
			}
		} catch (Exception eEx) {
			throw new ConnectorException("Expection trying to convert an jsonObject into a Map: "+eEx.getMessage());
		}
		return map;
	}

	/**
	 * Method to generate a list of lists to populate a Bonita table widget
	 * @param listOfMap A list of maps with all the data to show
	 * @return Reformated list
	 */
	private List<List<Object>> getBonitaTableList(
			List<Map<String, Object>> listOfMap) {
		List<List<Object>> table = new ArrayList<List<Object>>(listOfMap.size());
		List<Object> row;
		for (Map<String, Object> mapa : listOfMap) {
			row = new ArrayList<Object>(TITLES.size());
			for (String title : TITLES) {
				row.add(mapa.get(title));
			}
			table.add(row);

		}
		return table;
	}

	/**
	 * Method to generate the content of a CSV file
	 * @param list A list of maps with all the data to show
	 * @return String with the content of the file
	 */
	private String generateCSVFromList(List<Map<String, Object>> list) {
		String result = "";
		
		boolean first = true;
		for (String title : TITLES) {
			if (!first){
				result = result + CSV_SEPARATOR + title;
			} else {
				result = title;
				first = false;
			}
		}
		result += "\n";
		first = true;
		for (Map<String, Object> row : list) {
			for (String title : TITLES) {
				if (!first){
					result = result + CSV_SEPARATOR + row.get(title);
				} else {
					result = result + row.get(title);
					first = false;
				}
			}
			result += "\n";
			first = true;
		}
		return result.replaceAll("null", "");
	}
}