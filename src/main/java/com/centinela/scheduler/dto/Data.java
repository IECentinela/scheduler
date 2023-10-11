package com.centinela.scheduler.dto;

import java.util.Date;

public class Data {
	
	private String _id;
    private String asyncOps[];
    private String binders[];
    private Integer bookmarks;
    private Counts counts;
    private int createdAt;
    private int favorites;
    private String hashtags[];
    private String images[];
    private String inReplyTo;
    private String inReplyToId;
    private String lang;
    private String links[];
    private String mentions[];
    private RawLocation rawLocation;
    private int retweets;
    private Sentiment sentiment;
    private String source;
    private String text;
    private String type[];
    private Date updatedAt;
    private User user;
    private Object videos[];
    
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String[] getAsyncOps() {
		return asyncOps;
	}
	public void setAsyncOps(String[] asyncOps) {
		this.asyncOps = asyncOps;
	}
	public String[] getBinders() {
		return binders;
	}
	public void setBinders(String[] binders) {
		this.binders = binders;
	}
	public Counts getCounts() {
		return counts;
	}
	public void setCounts(Counts counts) {
		this.counts = counts;
	}
	public int getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(int createdAt) {
		this.createdAt = createdAt;
	}
	public int getFavorites() {
		return favorites;
	}
	public void setFavorites(int favorites) {
		this.favorites = favorites;
	}
	public String[] getHashtags() {
		return hashtags;
	}
	public void setHashtags(String[] hashtags) {
		this.hashtags = hashtags;
	}
	public String[] getImages() {
		return images;
	}
	public void setImages(String[] images) {
		this.images = images;
	}
	public String getInReplyTo() {
		return inReplyTo;
	}
	public void setInReplyTo(String inReplyTo) {
		this.inReplyTo = inReplyTo;
	}
	public String getInReplyToId() {
		return inReplyToId;
	}
	public void setInReplyToId(String inReplyToId) {
		this.inReplyToId = inReplyToId;
	}
	public String getLang() {
		return lang;
	}
	public void setLang(String lang) {
		this.lang = lang;
	}
	public String[] getLinks() {
		return links;
	}
	public void setLinks(String[] links) {
		this.links = links;
	}
	public String[] getMentions() {
		return mentions;
	}
	public void setMentions(String[] mentions) {
		this.mentions = mentions;
	}
	public RawLocation getRawLocation() {
		return rawLocation;
	}
	public void setRawLocation(RawLocation rawLocation) {
		this.rawLocation = rawLocation;
	}
	public int getRetweets() {
		return retweets;
	}
	public void setRetweets(int retweets) {
		this.retweets = retweets;
	}
	public Sentiment getSentiment() {
		return sentiment;
	}
	public void setSentiment(Sentiment sentiment) {
		this.sentiment = sentiment;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String[] getType() {
		return type;
	}
	public void setType(String[] type) {
		this.type = type;
	}
	public Date getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public Object[] getVideos() {
		return videos;
	}
	public void setVideos(Object[] videos) {
		this.videos = videos;
	}

}
