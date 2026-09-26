package io.chronicle.timeline.domain;

import io.chronicle.platform.DomainRecord;

public class TlNationState extends DomainRecord {
    private static final long serialVersionUID = 1L;

    private Long stateId;
    private Long worldlineId;
    private String countryName;
    private Integer recordYear;
    private String government;
    private String economyStatus;
    private String militaryStatus;
    private String diplomacyStatus;
    private String socialConflict;
    private String aiSummary;

    public Long getStateId() {
        return stateId;
    }

    public void setStateId(Long stateId) {
        this.stateId = stateId;
    }

    public Long getWorldlineId() {
        return worldlineId;
    }

    public void setWorldlineId(Long worldlineId) {
        this.worldlineId = worldlineId;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public Integer getRecordYear() {
        return recordYear;
    }

    public void setRecordYear(Integer recordYear) {
        this.recordYear = recordYear;
    }

    public String getGovernment() {
        return government;
    }

    public void setGovernment(String government) {
        this.government = government;
    }

    public String getEconomyStatus() {
        return economyStatus;
    }

    public void setEconomyStatus(String economyStatus) {
        this.economyStatus = economyStatus;
    }

    public String getMilitaryStatus() {
        return militaryStatus;
    }

    public void setMilitaryStatus(String militaryStatus) {
        this.militaryStatus = militaryStatus;
    }

    public String getDiplomacyStatus() {
        return diplomacyStatus;
    }

    public void setDiplomacyStatus(String diplomacyStatus) {
        this.diplomacyStatus = diplomacyStatus;
    }

    public String getSocialConflict() {
        return socialConflict;
    }

    public void setSocialConflict(String socialConflict) {
        this.socialConflict = socialConflict;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}
