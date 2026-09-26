package io.chronicle.timeline.domain;

import io.chronicle.platform.DomainRecord;

public class TlEvent extends DomainRecord {
    private static final long serialVersionUID = 1L;

    private Long eventId;
    private Long worldlineId;
    private Long screenshotId;
    private Long stageId;
    private String eventTitle;
    private String eventDate;
    private Integer eventYear;
    private String eventType;
    private String country;
    private String relatedForces;
    private String importanceLevel;
    private String divergenceFlag;
    private String summary;
    private String aiDescription;
    private String impactAnalysis;
    private String novelPotential;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getWorldlineId() {
        return worldlineId;
    }

    public void setWorldlineId(Long worldlineId) {
        this.worldlineId = worldlineId;
    }

    public Long getScreenshotId() {
        return screenshotId;
    }

    public void setScreenshotId(Long screenshotId) {
        this.screenshotId = screenshotId;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public Integer getEventYear() {
        return eventYear;
    }

    public void setEventYear(Integer eventYear) {
        this.eventYear = eventYear;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRelatedForces() {
        return relatedForces;
    }

    public void setRelatedForces(String relatedForces) {
        this.relatedForces = relatedForces;
    }

    public String getImportanceLevel() {
        return importanceLevel;
    }

    public void setImportanceLevel(String importanceLevel) {
        this.importanceLevel = importanceLevel;
    }

    public String getDivergenceFlag() {
        return divergenceFlag;
    }

    public void setDivergenceFlag(String divergenceFlag) {
        this.divergenceFlag = divergenceFlag;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getAiDescription() {
        return aiDescription;
    }

    public void setAiDescription(String aiDescription) {
        this.aiDescription = aiDescription;
    }

    public String getImpactAnalysis() {
        return impactAnalysis;
    }

    public void setImpactAnalysis(String impactAnalysis) {
        this.impactAnalysis = impactAnalysis;
    }

    public String getNovelPotential() {
        return novelPotential;
    }

    public void setNovelPotential(String novelPotential) {
        this.novelPotential = novelPotential;
    }
}
