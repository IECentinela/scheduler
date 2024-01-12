package com.centinela.scheduler.dto;

import lombok.Data;

@Data
public class Counts {
	
	private Integer textLength;
	private Integer sentiment;
	private Integer retweets;
	private Integer totalRetweets;
	private Integer favorites;
	private Integer hashtags;
	private Integer images;
	private Integer links;
	private Integer linksAndImages;
	private Integer mentions;
	private Integer quotes;
	private Integer impressions;
	private Integer totalReplies;
	private Integer bookmarks;
	private Integer originals;
	private Integer clears;
	private Integer replies;
	private Integer publicationScore;
	private Integer userValue;
	private Double tweetValue;
	private Integer lists;
	private Integer statuses;

}
