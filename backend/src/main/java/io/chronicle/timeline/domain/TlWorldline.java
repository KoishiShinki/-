package io.chronicle.timeline.domain;

import io.chronicle.platform.DomainRecord;

public class TlWorldline extends DomainRecord {
    private static final long serialVersionUID = 1L;

    private Long worldlineId;
    private Long creatorId;

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long id) {
        creatorId = id;
    }

    private String worldlineName;
    private String gameName;
    private String mainCountry;
    private Integer startYear;
    private Integer currentYear;
    private String narrativeStyle;
    private String description;
    private String coverUrl;
    private String visibility;
    private Integer divergenceScore;

    private Integer eventCount;
    private Integer stageCount;
    private Integer chapterCount;
    private Integer illustrationCount;

    public Long getWorldlineId() {
        return worldlineId;
    }

    public void setWorldlineId(Long worldlineId) {
        this.worldlineId = worldlineId;
    }

    public String getWorldlineName() {
        return worldlineName;
    }

    public void setWorldlineName(String worldlineName) {
        this.worldlineName = worldlineName;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getMainCountry() {
        return mainCountry;
    }

    public void setMainCountry(String mainCountry) {
        this.mainCountry = mainCountry;
    }

    public Integer getStartYear() {
        return startYear;
    }

    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    public Integer getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(Integer currentYear) {
        this.currentYear = currentYear;
    }

    public String getNarrativeStyle() {
        return narrativeStyle;
    }

    public void setNarrativeStyle(String narrativeStyle) {
        this.narrativeStyle = narrativeStyle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Integer getDivergenceScore() {
        return divergenceScore;
    }

    public void setDivergenceScore(Integer divergenceScore) {
        this.divergenceScore = divergenceScore;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public Integer getStageCount() {
        return stageCount;
    }

    public void setStageCount(Integer stageCount) {
        this.stageCount = stageCount;
    }

    public Integer getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(Integer chapterCount) {
        this.chapterCount = chapterCount;
    }

    public Integer getIllustrationCount() {
        return illustrationCount;
    }

    public void setIllustrationCount(Integer illustrationCount) {
        this.illustrationCount = illustrationCount;
    }
}
