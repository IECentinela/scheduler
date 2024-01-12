package com.centinela.scheduler.service.impl;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.Arrays;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.centinela.scheduler.ExecuteTasks;
import com.centinela.scheduler.dto.CreateLiveReport;
import com.centinela.scheduler.dto.FTPCredentials;
import com.centinela.scheduler.dto.RSLiveReport;
import com.centinela.scheduler.service.TweetService;
import com.centinela.scheduler.util.GeneraReportesManual;
import com.centinela.scheduler.util.URLConstants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

@Service
public class TweetServiceImpl implements TweetService {

	public static final Charset UTF_8 = Charset.forName("UTF-8");

	private final EntityManager entityManager;

	@Autowired
	RestTemplate restTemplate;

	@Value("${tweet.url}")
	private final String tweetApiUrl;

	@Value("${tweet.user}")
	private final String user;

	@Value("${tweet.password}")
	private final String password;
	
	@Value("${tweet.urlStats}")
	private final String urlStats;
	
	@Value("${ftp.serverFTP}")
	private final String serverFTP;

	@Value("${ftp.portFTP}")
	private final int portFTP;

	@Value("${ftp.usernameFTP}")
	private final String usernameFTP;

	@Value("${ftp.passwordFTP}")
	private final String passwordFTP;

	public TweetServiceImpl(RestTemplate restTemplate, EntityManager entityManager) {
		this.restTemplate = restTemplate;
		this.entityManager = entityManager;
		this.tweetApiUrl = "";
		this.user = "";
		this.password = "";
		this.urlStats = "";
		this.serverFTP = "";
		this.portFTP = 0;
		this.usernameFTP = "";
		this.passwordFTP = "";
	}

	@Override
	public String createLiveReport(CreateLiveReport liveReport, String url) {

		String respuesta = "";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String authHeader = "Bearer " + ExecuteTasks.dameToken(); // Agregar token
		headers.put("Authorization", Arrays.asList(authHeader));

		Gson gson = new Gson();
		String body = gson.toJson(liveReport);

		JsonObject jsonObject = gson.fromJson(body, JsonObject.class);

		// Eliminar limit del json
		jsonObject.remove("limit");
		jsonObject.remove("idProyecto");
		jsonObject.remove("in_startDate");
		jsonObject.remove("in_endDate");
		jsonObject.remove("startDateProject");
		jsonObject.remove("endDateProject");
		if (jsonObject.has("query")) {
			JsonObject queryObject = jsonObject.getAsJsonObject("query");
			queryObject.remove("limit");
		}

		String modifiedJson = gson.toJson(jsonObject);

		ResponseEntity<RSLiveReport> response = null;

		HttpEntity<String> requestEntity = new HttpEntity<>(modifiedJson, headers);

		String urlApi = tweetApiUrl != "" ? tweetApiUrl : url;
		try {

			response = restTemplate.exchange(urlApi + URLConstants.URL_7_DAYS, HttpMethod.POST, requestEntity, RSLiveReport.class);
			System.out.println("Respuesta servicio 7-days: "+response);
		} catch (Exception e) {

			System.out.println("Crear Reporte " + e.getMessage());
			return "ERROR AL GENERAR REPORTE";

		}

		if (response.getStatusCode() == HttpStatus.OK) {

			StoredProcedureQuery storedProcedureQuery = entityManager
					.createStoredProcedureQuery("centineladb.sp_saveQueryReportBinder");
			storedProcedureQuery.registerStoredProcedureParameter("in_queryreport", String.class, ParameterMode.IN);
			storedProcedureQuery.registerStoredProcedureParameter("in_resourceid", String.class, ParameterMode.IN);
			storedProcedureQuery.registerStoredProcedureParameter("in_typereport", Integer.class, ParameterMode.IN);
			storedProcedureQuery.registerStoredProcedureParameter("in_fkproject", Integer.class, ParameterMode.IN);
			storedProcedureQuery.registerStoredProcedureParameter("in_startDate", String.class, ParameterMode.IN);
			storedProcedureQuery.registerStoredProcedureParameter("in_endDate", String.class, ParameterMode.IN);
			storedProcedureQuery.registerStoredProcedureParameter("response", String.class, ParameterMode.OUT);

			storedProcedureQuery.setParameter("in_queryreport", modifiedJson);
			storedProcedureQuery.setParameter("in_resourceid", response.getBody().getResourceId());
			storedProcedureQuery.setParameter("in_typereport", 2);
			storedProcedureQuery.setParameter("in_fkproject", liveReport.getIdProyecto());
			storedProcedureQuery.setParameter("in_startDate", liveReport.getStartDateProject());
			storedProcedureQuery.setParameter("in_endDate", liveReport.getEndDateProject());

			storedProcedureQuery.execute();

			respuesta = (String) storedProcedureQuery.getOutputParameterValue("response");
			System.out.println("Respuesta SP sp_saveQueryReportBinder: " + respuesta);

		}
		return respuesta;
	}

	@Override
	public String getStats(int idProyecto, String reportId, String url) {
		
		String respuesta = null;
		String[] idCorto = reportId.split("-");
		
		Unirest.setTimeouts(0, 0);
		try {
			System.out.println("URL getStats: "+(urlStats != "" ? urlStats : "https://s3.eu-west-1.amazonaws.com/stats.tweetbinder.com")+"/" + idCorto[0] + URLConstants.URL_GET_STATS);
			com.mashape.unirest.http.HttpResponse<String> response = Unirest.get((urlStats != "" ? urlStats : "https://s3.eu-west-1.amazonaws.com/stats.tweetbinder.com")+"/" + idCorto[0] + URLConstants.URL_GET_STATS).asString();
			System.out.println("Response: "+response.getBody());
			if (response.getBody() != null) {

				StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery("centineladb.sp_saveReportStats");
				storedProcedureQuery.registerStoredProcedureParameter("in_IDProject", int.class, ParameterMode.IN);
				storedProcedureQuery.registerStoredProcedureParameter("in_Stats", String.class, ParameterMode.IN);
				storedProcedureQuery.registerStoredProcedureParameter("response", String.class, ParameterMode.OUT);

				Gson gson = new Gson();
				String stadisticas = gson.toJson(response.getBody());
				byte[] ptext = stadisticas.getBytes(UTF_8);
				String value = new String(ptext, UTF_8);

				System.out.println("Value b: " + response.getBody().toString().replaceAll("\\n\\n", ""));
				
	            JSONObject jsonObject = new JSONObject(response.getBody().toString().replaceAll("\\n\\n", ""));

	            replaceNewlinesInTextFields(jsonObject);
	            
				System.out.println("jsonObject: "+ jsonObject);
				storedProcedureQuery.setParameter("in_IDProject", idProyecto);
				storedProcedureQuery.setParameter("in_Stats", jsonObject.toString());

				storedProcedureQuery.execute();

				respuesta = (String) storedProcedureQuery.getOutputParameterValue("response");
				System.out.println("Respuesta sp_saveReportStats del proyecto con ID: "+idProyecto+": " + respuesta);
			}
		} catch (UnirestException e) {
			System.out.println("Error al consumir/guardar getStats...");
			e.printStackTrace();
		}

		return respuesta;
	}
	
	
	
	public static void replaceNewlinesInTextFields(JSONObject jsonObject) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            if (value instanceof String) {
                // Si es una cadena, buscar campos "text" y reemplazar "\n\n"
                if ("text".equals(key)) {
                    String textValue = (String) value;
                    String updatedTextValue = textValue.replaceAll("\n\n", "");
                    String updatedTextValue2 = updatedTextValue.replaceAll("\n", "");
                    String updatedTextValue3 = updatedTextValue2.replaceAll("\"", "");
                    String updatedTextValue4 = updatedTextValue3.replaceAll("\\u2013", "");
                    String updatedTextValue5 = updatedTextValue4.replaceAll("\\u201c", "");
                    String updatedTextValue6 = updatedTextValue5.replaceAll("\\u2019", "");
                    String updatedTextValue7 = updatedTextValue6.replaceAll("\\u2026", "");
                    String updatedTextValue8 = updatedTextValue7.replaceAll("\\u201d", "");
                    String updatedTextValue9 = updatedTextValue8.replaceAll("\\u20e3", "");
                    String updatedTextValue10 = updatedTextValue9.replaceAll("'", "");
                    String updatedTextValue11 = updatedTextValue10.replaceAll("\\\\", "");
                    jsonObject.put(key, updatedTextValue11);
                }
            } else if (value instanceof JSONObject) {
                // Si es un objeto JSON, realizar la búsqueda de forma recursiva
                replaceNewlinesInTextFields((JSONObject) value);
            } else if (value instanceof JSONArray) {
                // Si es un arreglo JSON, buscar en cada elemento del arreglo
                JSONArray jsonArray = (JSONArray) value;
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object arrayItem = jsonArray.get(i);
                    if (arrayItem instanceof JSONObject) {
                        replaceNewlinesInTextFields((JSONObject) arrayItem);
                    }
                }
            }
        }
    }
	
	public static void encodeStringFieldsToUTF8(JSONObject jsonObject) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            if (value instanceof String) {
                // Si es una cadena, codificar en UTF-8
                String stringValue = (String) value;
                String utf8Value = new String(stringValue.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                jsonObject.put(key, utf8Value);
            } else if (value instanceof JSONObject) {
                // Si es un objeto JSON, realizar la codificación de forma recursiva
                encodeStringFieldsToUTF8((JSONObject) value);
            } else if (value instanceof JSONArray) {
                // Si es un arreglo JSON, codificar en UTF-8 cada elemento del arreglo
                JSONArray jsonArray = (JSONArray) value;
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object arrayItem = jsonArray.get(i);
                    if (arrayItem instanceof JSONObject) {
                        encodeStringFieldsToUTF8((JSONObject) arrayItem);
                    }
                }
            }
        }
    }
	
	@Override
	public String cargarGraphextPorRangoDeFecha(Date in_startDate, Date in_endDate, Integer idProyecto) {
		String respuesta = null;
		try {
			GeneraReportesManual abc = new GeneraReportesManual(entityManager);
			FTPCredentials ftp = new FTPCredentials();
			ftp.setServerFTP(serverFTP);
			ftp.setUsernameFTP(usernameFTP);
			ftp.setPasswordFTP(passwordFTP);
			ftp.setPortFTP(portFTP);
			respuesta = abc.cargarReportesPorProyectoFechas(in_startDate.toString(), in_endDate.toString(), idProyecto, ftp);
		} catch (Exception e) {
			respuesta = "{\"response\" : \"301\"}";
			e.printStackTrace();
		}
		return respuesta;
	}
}
