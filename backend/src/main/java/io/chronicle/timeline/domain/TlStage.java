package io.chronicle.timeline.domain;

import io.chronicle.platform.DomainRecord;

public class TlStage extends DomainRecord {
    private static final long serialVersionUID = 1L;

    private Long stageId;
    private Long worldlineId;
    private String stageName;
    private Integer startYear;
    private Integer endYear;
    private String stageTheme;
    private String stageSummary;
    private String novelStatus;
    private String imageStatus;
    private Integer eventCount;

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public Long getWorldlineId() {
        return worldlineId;
    }

    public void setWorldlineId(Long worldlineId) {
        this.worldlineId = worldlineId;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public Integer getStartYear() {
        return startYear;
    }

    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    public Integer getEndYear() {
        return endYear;
    }

    public void setEndYear(Integer endYear) {
        this.endYear = endYear;
    }

    public String getStageTheme() {
        return stageTheme;
    }

    public void setStageTheme(String stageTheme) {
        this.stageTheme = stageTheme;
    }

    public String getStageSummary() {
        return stageSummary;
    }

    public void setStageSummary(String stageSummary) {
        this.stageSummary = stageSummary;
    }

    public String getNovelStatus() {
        return novelStatus;
    }

    public void setNovelStatus(String novelStatus) {
        this.novelStatus = novelStatus;
    }

    public String getImageStatus() {
        return imageStatus;
    }

    public void setImageStatus(String imageStatus) {
        this.imageStatus = imageStatus;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }
}
