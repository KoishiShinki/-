package io.chronicle.timeline.domain;

import io.chronicle.platform.DomainRecord;

public class TlScreenshot extends DomainRecord {
    private static final long serialVersionUID = 1L;

    private Long screenshotId;
    private Long worldlineId;
    private String imageUrl;
    private String originalName;
    private String aiStatus;
    private String aiRawResult;
    private String draftEventJson;

    public Long getScreenshotId() {
        return screenshotId;
    }

    public void setScreenshotId(Long screenshotId) {
        this.screenshotId = screenshotId;
    }

    public Long getWorldlineId() {
        return worldlineId;
    }

    public void setWorldlineId(Long worldlineId) {
        this.worldlineId = worldlineId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getAiStatus() {
        return aiStatus;
    }

    public void setAiStatus(String aiStatus) {
        this.aiStatus = aiStatus;
    }

    public String getAiRawResult() {
        return aiRawResult;
    }

    public void setAiRawResult(String aiRawResult) {
        this.aiRawResult = aiRawResult;
    }

    public String getDraftEventJson() {
        return draftEventJson;
    }

    public void setDraftEventJson(String draftEventJson) {
        this.draftEventJson = draftEventJson;
    }
}
