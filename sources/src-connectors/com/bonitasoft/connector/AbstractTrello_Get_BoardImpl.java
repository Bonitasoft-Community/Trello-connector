package com.bonitasoft.connector;

import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorValidationException;

public abstract class AbstractTrello_Get_BoardImpl extends AbstractConnector {

	protected final static String BOARD_INPUT_PARAMETER = "board";
	protected final static String TOKEN_INPUT_PARAMETER = "token";
	protected final static String APIKEY_INPUT_PARAMETER = "apiKey";
	protected final String TRELLOLIST_OUTPUT_PARAMETER = "trelloList";
	protected final String BONITALIST_OUTPUT_PARAMETER = "bonitaList";
	protected final String BONITACOLUMN_OUTPUT_PARAMETER = "bonitaColumn";
	protected final String STRINGCSV_OUTPUT_PARAMETER = "stringCSV";

	protected final java.lang.String getBoard() {
		return (java.lang.String) getInputParameter(BOARD_INPUT_PARAMETER);
	}

	protected final java.lang.String getToken() {
		return (java.lang.String) getInputParameter(TOKEN_INPUT_PARAMETER);
	}

	protected final java.lang.String getApiKey() {
		return (java.lang.String) getInputParameter(APIKEY_INPUT_PARAMETER);
	}

	protected final void setTrelloList(java.util.List trelloList) {
		setOutputParameter(TRELLOLIST_OUTPUT_PARAMETER, trelloList);
	}

	protected final void setBonitaList(java.util.List bonitaList) {
		setOutputParameter(BONITALIST_OUTPUT_PARAMETER, bonitaList);
	}

	protected final void setBonitaColumn(java.util.List bonitaColumn) {
		setOutputParameter(BONITACOLUMN_OUTPUT_PARAMETER, bonitaColumn);
	}

	protected final void setStringCSV(java.lang.String stringCSV) {
		setOutputParameter(STRINGCSV_OUTPUT_PARAMETER, stringCSV);
	}

	@Override
	public void validateInputParameters() throws ConnectorValidationException {
		try {
			getBoard();
		} catch (ClassCastException cce) {
			throw new ConnectorValidationException("board type is invalid");
		}
		try {
			getToken();
		} catch (ClassCastException cce) {
			throw new ConnectorValidationException("token type is invalid");
		}
		try {
			getApiKey();
		} catch (ClassCastException cce) {
			throw new ConnectorValidationException("apiKey type is invalid");
		}

	}

}
