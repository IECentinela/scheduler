package com.centinela.scheduler.service.impl;

import java.sql.Date;
import java.util.Arrays;

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
import com.centinela.scheduler.dto.RSLiveReport;
import com.centinela.scheduler.service.TweetService;
import com.centinela.scheduler.util.URLConstants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;


@Service
public class TweetServiceImpl implements TweetService {

	private final EntityManager entityManager;
	
	@Autowired
	RestTemplate restTemplate;

	@Value("${tweet.url}")
	private final String tweetApiUrl;

	@Value("${tweet.user}")
	private final String user;

	@Value("${tweet.password}")
	private final String password;

	@Autowired
	public TweetServiceImpl(RestTemplate restTemplate, EntityManager entityManager) {
		this.restTemplate = restTemplate;
		this.entityManager = entityManager;
		this.tweetApiUrl = "";
		this.user = "";
		this.password = "";
	}

	
	@Override
	public String createLiveReport(CreateLiveReport liveReport, String url) {
		
		String respuesta = "";
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		String authHeader = "Bearer " + ExecuteTasks.dameToken(); //Agregar token
        headers.put("Authorization", Arrays.asList(authHeader));
        
        Gson gson = new Gson();
		String body = gson.toJson(liveReport);
		
        JsonObject jsonObject = gson.fromJson(body, JsonObject.class);

        //Eliminar limit del json
        jsonObject.remove("limit");
        jsonObject.remove("idProyecto");
        jsonObject.remove("in_startDate");
        jsonObject.remove("in_endDate");
        if (jsonObject.has("query")) {
            JsonObject queryObject = jsonObject.getAsJsonObject("query");
            queryObject.remove("limit");
        }

        String modifiedJson = gson.toJson(jsonObject);
		
		ResponseEntity<RSLiveReport> response = null;

		HttpEntity<String> requestEntity = new HttpEntity<>(modifiedJson, headers);
		
		String urlApi = tweetApiUrl != "" ? tweetApiUrl : url ;
		try {
			
			response = restTemplate.exchange(urlApi + URLConstants.URL_7_DAYS, HttpMethod.POST, requestEntity, RSLiveReport.class);
		
		}catch(Exception e) {
			
			System.out.println("Crear Reporte "+e.getMessage());
			return "ERROR AL GENERAR REPORTE";
			
		}
		
		if(response.getStatusCode() == HttpStatus.OK) {
			
			StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery("centineladb.sp_saveQueryReportBinder");
	        storedProcedureQuery.registerStoredProcedureParameter("in_queryreport", String.class, ParameterMode.IN);
	        storedProcedureQuery.registerStoredProcedureParameter("in_resourceid", String.class, ParameterMode.IN);
	        storedProcedureQuery.registerStoredProcedureParameter("in_typereport", Integer.class, ParameterMode.IN);
	        storedProcedureQuery.registerStoredProcedureParameter("in_fkproject", Integer.class, ParameterMode.IN);
	        storedProcedureQuery.registerStoredProcedureParameter("in_startDate", Date.class, ParameterMode.IN);
	        storedProcedureQuery.registerStoredProcedureParameter("in_endDate", Date.class, ParameterMode.IN);
	        storedProcedureQuery.registerStoredProcedureParameter("response", String.class, ParameterMode.OUT);
	        
	        storedProcedureQuery.setParameter("in_queryreport",body);
	        storedProcedureQuery.setParameter("in_resourceid",response.getBody().getResourceId());
	        storedProcedureQuery.setParameter("in_typereport",2);
	        storedProcedureQuery.setParameter("in_fkproject",liveReport.getIdProyecto());
	        storedProcedureQuery.setParameter("in_startDate",liveReport.getIn_startDate());
	        storedProcedureQuery.setParameter("in_endDate",liveReport.getIn_endDate());
	        
	        storedProcedureQuery.execute();
	        
	        respuesta = (String) storedProcedureQuery.getOutputParameterValue("response");
	        System.out.println("Respuesta SP: "+respuesta);
			
		}	
		return respuesta;
	}

	

}
