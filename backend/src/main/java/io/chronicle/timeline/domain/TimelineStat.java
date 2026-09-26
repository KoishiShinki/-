package io.chronicle.timeline.domain;

import java.io.Serializable;

public class TimelineStat implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long worldlineId;
    private Integer eventCount;
    private Integer stageCount;
    private Integer chapterCount;
    private Integer illustrationCount;
    private Integer divergenceScore;

    public Long getWorldlineId() {
        return worldlineId;
    }

    public void setWorldlineId(Long worldlineId) {
        this.worldlineId = worldlineId;
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

    public Integer getDivergenceScore() {
        return divergenceScore;
    }

    public void setDivergenceScore(Integer divergenceScore) {
        this.divergenceScore = divergenceScore;
    }
}
