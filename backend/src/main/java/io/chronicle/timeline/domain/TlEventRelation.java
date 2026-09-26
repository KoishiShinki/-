package io.chronicle.timeline.domain;

import io.chronicle.platform.DomainRecord;

public class TlEventRelation extends DomainRecord {
    private static final long serialVersionUID = 1L;

    private Long relationId;
    private Long worldlineId;
    private Long sourceEventId;
    private Long targetEventId;
    private String relationType;
    private String relationDesc;
    private String aiGenerated;

    public Long getRelationId() {
        return relationId;
    }

    public void setRelationId(Long relationId) {
        this.relationId = relationId;
    }

    public Long getWorldlineId() {
        return worldlineId;
    }

    public void setWorldlineId(Long worldlineId) {
        this.worldlineId = worldlineId;
    }

    public Long getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(Long sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Long getTargetEventId() {
        return targetEventId;
    }

    public void setTargetEventId(Long targetEventId) {
        this.targetEventId = targetEventId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getRelationDesc() {
        return relationDesc;
    }

    public void setRelationDesc(String relationDesc) {
        this.relationDesc = relationDesc;
    }

    public String getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(String aiGenerated) {
        this.aiGenerated = aiGenerated;
    }
}
