package io.chronicle.timeline.domain;

import io.chronicle.platform.DomainRecord;

public class TlIllustration extends DomainRecord {
    private static final long serialVersionUID = 1L;

    private Long illustrationId;
    private Long worldlineId;
    private Long stageId;
    private Long eventId;
    private Long chapterId;
    private Long characterId;
    private String illustrationTitle;
    private String illustrationType;
    private String imageUrl;
    private String prompt;
    private String negativePrompt;
    private String styleType;
    private String generateStatus;
    private String aiRawResult;
    private String sceneDescription;

    public Long getIllustrationId() {
        return illustrationId;
    }

    public void setIllustrationId(Long illustrationId) {
        this.illustrationId = illustrationId;
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

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Long getCharacterId() {
        return characterId;
    }

    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
    }

    public String getIllustrationTitle() {
        return illustrationTitle;
    }

    public void setIllustrationTitle(String illustrationTitle) {
        this.illustrationTitle = illustrationTitle;
    }

    public String getIllustrationType() {
        return illustrationType;
    }

    public void setIllustrationType(String illustrationType) {
        this.illustrationType = illustrationType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getNegativePrompt() {
        return negativePrompt;
    }

    public void setNegativePrompt(String negativePrompt) {
        this.negativePrompt = negativePrompt;
    }

    public String getStyleType() {
        return styleType;
    }

    public void setStyleType(String styleType) {
        this.styleType = styleType;
    }

    public String getGenerateStatus() {
        return generateStatus;
    }

    public void setGenerateStatus(String generateStatus) {
        this.generateStatus = generateStatus;
    }

    public String getAiRawResult() {
        return aiRawResult;
    }

    public void setAiRawResult(String aiRawResult) {
        this.aiRawResult = aiRawResult;
    }

    public String getSceneDescription() {
        return sceneDescription;
    }

    public void setSceneDescription(String sceneDescription) {
        this.sceneDescription = sceneDescription;
    }
}
