package com.centinela.scheduler.service;

import com.centinela.scheduler.dto.CreateLiveReport;

public interface TweetService {

	public String createLiveReport(CreateLiveReport liveReport, String url);

}
