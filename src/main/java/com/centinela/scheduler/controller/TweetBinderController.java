package com.centinela.scheduler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.centinela.scheduler.dto.CreateLiveReport;
import com.centinela.scheduler.dto.GetStats;
import com.centinela.scheduler.service.TweetService;

@CrossOrigin
@RestController
@RequestMapping("/schedule")
public class TweetBinderController {
	@Autowired
	TweetService tweetService;
	
	@PostMapping(value = "/getStats", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createLiveReport (@RequestBody GetStats getStats){
		
		String response = tweetService.getStats(getStats.getIdProyecto(), getStats.getReportId(), getStats.getUrl());
		
		return new ResponseEntity<String>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/cargarGraphextPorRangoDeFecha", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> cargarGraphextPorRangoDeFecha (@RequestBody CreateLiveReport clr){
		
		String response = tweetService.cargarGraphextPorRangoDeFecha(clr.getIn_startDate(), clr.getIn_endDate(), clr.getIdProyecto());
		
		return new ResponseEntity<String>(response, HttpStatus.OK);
	}
}
