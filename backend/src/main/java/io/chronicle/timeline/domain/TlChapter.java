package io.chronicle.timeline.domain;

import io.chronicle.platform.DomainRecord;

public class TlChapter extends DomainRecord {
    private static final long serialVersionUID = 1L;

    private Long chapterId;
    private Long worldlineId;
    private Long stageId;
    private String chapterTitle;
    private Integer chapterOrder;
    private String relatedEventIds;
    private String writingStyle;
    private String content;
    private String aiPrompt;
    private String status;
    private Long coverIllustrationId;
    private String coverUrl;

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Long getWorldlineId() {
        return worldlineId;
    }

    public void setWorldlineId(Long worldlineId) {
        this.worldlineId = worldlineId;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public Integer getChapterOrder() {
        return chapterOrder;
    }

    public void setChapterOrder(Integer chapterOrder) {
        this.chapterOrder = chapterOrder;
    }

    public String getRelatedEventIds() {
        return relatedEventIds;
    }

    public void setRelatedEventIds(String relatedEventIds) {
        this.relatedEventIds = relatedEventIds;
    }

    public String getWritingStyle() {
        return writingStyle;
    }

    public void setWritingStyle(String writingStyle) {
        this.writingStyle = writingStyle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAiPrompt() {
        return aiPrompt;
    }

    public void setAiPrompt(String aiPrompt) {
        this.aiPrompt = aiPrompt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCoverIllustrationId() {
        return coverIllustrationId;
    }

    public void setCoverIllustrationId(Long coverIllustrationId) {
        this.coverIllustrationId = coverIllustrationId;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}
