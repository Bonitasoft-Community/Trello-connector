/**
 * 
 */
package com.bonitasoft.connector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

	private static final String GET_TRELLO_BOARDS_URL = "https://trello.com/1/members/me/boards?key=%s&token=%s";
	private static final String GET_TRELLO_BOARD_URL = "https://api.trello.com/1/boards/%s?lists=open&list_fields=name,desc&cards=open&fields=name,desc,labelNames&key=%s&token=%s";
	private static Logger logger = Logger.getLogger(Trello_Get_BoardImpl.class.getName());
	private static Map<String, String> lists;
	private static JSONObject labelNames;
	private static List<String> titles;
	private static String[] boards;
	private static String[] boardsId;

	@Override
	protected void executeBusinessLogic() throws ConnectorException {
		// Get access to the connector input parameters
		// getBoard();
		// getToken();
		// getApiKey();
		initData();
		try {
			HttpClient client = new HttpClient();
			HttpMethod method = null;
			// method = new GetMethod(String.format(GET_TRELLO_BOARDS_URL,
			// getApiKey(), getToken()));
			method = new GetMethod(String.format(GET_TRELLO_BOARD_URL,
					boards[0], getApiKey(), getToken()));
			method.addRequestHeader(new Header("Content-Type",
					"application/json; charset=UTF-8"));
			client.executeMethod(method);
			JSONArray array = getJSONFromResponse(method);
			method.releaseConnection();

			List<Map<String, Object>> result = generateCSV(array);
			setTrelloList(result);
			setBonitaColumn(titles);
			setBonitaList(getBonitaTableList(result));
			setStringCSV(generarCSVdesdeLista(result));
		} catch (Exception e) {
			throw new ConnectorException(e.getMessage());
		}
	}

	private void initData() {
		logger.severe("INIT DATA");
		String board = getBoard();
		if (board != null && !board.equals("")){
			boards = new String[] { board };
			logger.severe("Retrieve data from only this board: "+board);
		}else {
			try {
				HttpClient client = new HttpClient();
				HttpMethod method = new GetMethod(String.format(
						GET_TRELLO_BOARDS_URL, getApiKey(), getToken()));
				method.addRequestHeader(new Header("Content-Type",
						"application/json; charset=UTF-8"));
				client.executeMethod(method);
				
				
				JSONArray array = getJSONFromResponse(method);
				logger.severe(array.toString());
				method.releaseConnection();
				JSONObject obj;
				boards = new String[array.length()];
				boardsId = new String[array.length()];
				for(int i=0; i<array.length();i++){
					obj = array.getJSONObject(i);
					boardsId[i] = obj.getString("id");
//					boards[i] = obj.getString("name");
					boards[i] = obj.getString("id");
				}
				logger.severe("Retrieve data from these board: "+ boards);
				boards = new String[] { "kx8lH0k8" };
			} catch (Exception e) {
				//boards = new String[] { "kx8lH0k8" };
				new ConnectorValidationException(
						"Error trying to get boards names: "+e.getMessage());
				
			}
		}
		
	}

	@Override
	public void validateInputParameters() throws ConnectorValidationException {
		// TODO Apéndice de método generado automáticamente
		super.validateInputParameters();
		
	}

	public List<Map<String, Object>> generateCSV(JSONArray array)
			throws Exception {

		Map<String, Object> mapa;
		List<Map<String, Object>> lista = new ArrayList<Map<String, Object>>();
		JSONObject obj = (JSONObject) array.get(0);
		System.out.println(obj);
		labelNames = obj.getJSONObject("labelNames");
		lists = listsToMap(obj.getJSONArray("lists"));
		JSONArray cards = obj.getJSONArray("cards");
		JSONArray tempLabels;
		JSONObject card;

		titles = new ArrayList<String>();
		titles.add("yellow");
		titles.add("red");
		titles.add("purple");
		titles.add("green");
		titles.add("orange");
		titles.add("blue");
		titles.add("name");
		titles.add("desc");
		titles.add("due");
		titles.add("list");
		titles.add("shortUrl");

		for (int i = 0; i < cards.length(); i++) {

			card = cards.getJSONObject(i);

			mapa = new HashMap<String, Object>();
			mapa.put("name", card.getString("name"));
			mapa.put("desc", getDescFormated(card.getString("desc")));
			try {
				mapa.put("due", card.getString("due"));
			} catch (Exception e) {
				mapa.put("due", "");
			}
			mapa.put("list", lists.get(card.getString("idList")));
			mapa.put("shortUrl", card.getString("shortUrl"));

			tempLabels = card.getJSONArray("labels");
			String color;
			for (int j = 0; j < tempLabels.length(); j++) {
				color = tempLabels.getJSONObject(j).getString("color");
				mapa.put(color, labelNames.getString(color));
			}

			lista.add(mapa);

		}
		return lista;
	}

	private static String getDescFormated(String string) {

		return string.replaceAll("(\\r|\\n)", " ");
	}

	private static Map<String, String> listsToMap(JSONArray lists)
			throws JSONException {
		Map<String, String> mapa = new HashMap<String, String>();
		JSONObject tObj;

		for (int i = 0; i < lists.length(); i++) {
			tObj = lists.getJSONObject(i);
			mapa.put(tObj.getString("id"), tObj.getString("name"));
		}
		return mapa;

	}

	private JSONArray getJSONFromResponse(HttpMethod method) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				method.getResponseBodyAsStream()));
		StringBuilder builder = new StringBuilder();

		for (String line = null; (line = reader.readLine()) != null;) {
			builder.append(line).append("\n");
		}
		
		JSONTokener tokener = new JSONTokener(builder.toString());
		JSONObject temp = null;
		JSONArray tArray = null;
		try {
			temp = new JSONObject(tokener);
			tArray = new JSONArray();
			tArray.put(temp);
			// logger.severe("You cant stay here " + temp.toString());
		} catch (JSONException e) {
			tokener = new JSONTokener(builder.toString());
			tArray = new JSONArray(tokener);
		}
		return tArray;
	}

	private List<Map<String, Object>> jsonArrayTOList(JSONArray tArray) {
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();

		JSONObject tO = null;

		for (int i = 0; i < tArray.length(); i++) {
			try {
				tO = tArray.getJSONObject(i);
				res.add(jsonObjectTOMap(tO));
			} catch (Exception e) {
				logger.severe("List<List> ?????: " + e.getMessage());
			}
		}
		return res;
	}

	private Map<String, Object> jsonObjectTOMap(JSONObject obj) {
		String[] names = JSONObject.getNames(obj);
		Map<String, Object> map = new HashMap<String, Object>();
		JSONArray tA = null;
		JSONObject tO;
		try {
			for (int i = 0; i < names.length; i++) {
				try {
					tA = obj.getJSONArray(names[i]);
					map.put(names[i], jsonArrayTOList(tA));
				} catch (Exception e) {
					try {
						tO = obj.getJSONObject(names[i]);
						map.put(names[i], jsonObjectTOMap(tO));
					} catch (Exception e2) {
						map.put(names[i], obj.get(names[i]));
					}

				}
			}
		} catch (Exception eEx) {
			logger.severe("Check This Exception");
		}
		return map;
	}

	private List<List<Object>> getBonitaTableList(
			List<Map<String, Object>> listaMapa) {
		List<List<Object>> table = new ArrayList<List<Object>>();
		List<Object> row;
		for (Map<String, Object> mapa : listaMapa) {
			row = new ArrayList<Object>();
			for (String title : titles) {
				row.add(mapa.get(title));
			}
			table.add(row);

		}
		return table;
	}

	private static String generarCSVdesdeLista(List<Map<String, Object>> lista) {
		String result = "";

		// Set<String> titles = lista.get(0).keySet();
		boolean first = true;
		for (String title : titles) {
			if (!first)
				result = result + ";" + title;
			else {
				result = title;
				first = false;
			}
		}
		result = result + "\n";
		first = true;
		for (Map<String, Object> row : lista) {
			for (String title : titles) {
				if (!first)
					result = result + ";" + row.get(title);
				else {
					result = result + row.get(title);
					first = false;
				}
			}
			result = result + "\n";
			first = true;
		}
		return result.replaceAll("null", "");
	}
}