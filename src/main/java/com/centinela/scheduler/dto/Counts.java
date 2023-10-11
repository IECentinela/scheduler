package com.centinela.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class Counts {
	
	private int textLength;
	private int sentiment;
	private int retweets;
	private int totalRetweets;
	private int favorites;
	private int hashtags;
	private int images;
	private int links;
	private int linksAndImages;
	private int mentions;
	private int quotes;
	private int impressions;
	private int totalReplies;
	private int bookmarks;
	private int originals;
	private int clears;
	private int replies;
	private int publicationScore;
	private float userValue;
	private float tweetValue;
	private int lists;
	private int statuses;

}
