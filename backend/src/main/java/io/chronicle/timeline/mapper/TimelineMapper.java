package io.chronicle.timeline.mapper;

import io.chronicle.timeline.domain.TimelineStat;
import io.chronicle.timeline.domain.TlAiTask;
import io.chronicle.timeline.domain.TlChapter;
import io.chronicle.timeline.domain.TlCharacter;
import io.chronicle.timeline.domain.TlEvent;
import io.chronicle.timeline.domain.TlEventRelation;
import io.chronicle.timeline.domain.TlIllustration;
import io.chronicle.timeline.domain.TlNationState;
import io.chronicle.timeline.domain.TlScreenshot;
import io.chronicle.timeline.domain.TlStage;
import io.chronicle.timeline.domain.TlUserProfile;
import io.chronicle.timeline.domain.TlWorldline;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface TimelineMapper {
    List<TlWorldline> selectWorldlineList(TlWorldline worldline);

    List<TlWorldline> selectPublicWorldlineList(TlWorldline worldline);

    List<TlWorldline> selectMyWorldlineList(@Param("createBy") String createBy);

    TlWorldline selectWorldlineById(Long worldlineId);

    int insertWorldline(TlWorldline worldline);

    int updateWorldline(TlWorldline worldline);

    int deleteWorldlineByIds(Long[] worldlineIds);

    int deleteWorldlineById(Long worldlineId);

    int updateWorldlineCover(
            @Param("worldlineId") Long worldlineId,
            @Param("coverUrl") String coverUrl,
            @Param("updateBy") String updateBy);

    int updateWorldlineVisibility(
            @Param("worldlineId") Long worldlineId,
            @Param("visibility") String visibility,
            @Param("updateBy") String updateBy);

    int updateWorldlineDivergence(
            @Param("worldlineId") Long worldlineId,
            @Param("divergenceScore") Integer divergenceScore);

    TimelineStat selectWorldlineStat(Long worldlineId);

    List<TlScreenshot> selectScreenshotList(@Param("worldlineId") Long worldlineId);

    TlScreenshot selectScreenshotById(Long screenshotId);

    int insertScreenshot(TlScreenshot screenshot);

    int updateScreenshot(TlScreenshot screenshot);

    int deleteScreenshotById(Long screenshotId);

    List<TlEvent> selectEventList(TlEvent event);

    TlEvent selectEventById(Long eventId);

    int insertEvent(TlEvent event);

    int updateEvent(TlEvent event);

    int deleteEventById(Long eventId);

    int updateEventStage(
            @Param("eventId") Long eventId,
            @Param("stageId") Long stageId,
            @Param("updateBy") String updateBy);

    int markEventDivergence(@Param("eventId") Long eventId, @Param("updateBy") String updateBy);

    List<TlStage> selectStageList(@Param("worldlineId") Long worldlineId);

    TlStage selectStageById(Long stageId);

    int insertStage(TlStage stage);

    int updateStage(TlStage stage);

    int deleteStageById(Long stageId);

    List<TlNationState> selectNationStateList(@Param("worldlineId") Long worldlineId);

    TlNationState selectNationStateById(Long stateId);

    int insertNationState(TlNationState state);

    int updateNationState(TlNationState state);

    int deleteNationStateById(Long stateId);

    List<TlCharacter> selectCharacterList(@Param("worldlineId") Long worldlineId);

    TlCharacter selectCharacterById(Long characterId);

    int insertCharacter(TlCharacter character);

    int updateCharacter(TlCharacter character);

    int deleteCharacterById(Long characterId);

    List<TlChapter> selectChapterList(
            @Param("worldlineId") Long worldlineId, @Param("publicOnly") Boolean publicOnly);

    TlChapter selectChapterById(Long chapterId);

    int insertChapter(TlChapter chapter);

    int updateChapter(TlChapter chapter);

    int deleteChapterById(Long chapterId);

    int setChapterPublic(
            @Param("chapterId") Long chapterId,
            @Param("status") String status,
            @Param("updateBy") String updateBy);

    int setChapterCover(
            @Param("chapterId") Long chapterId,
            @Param("illustrationId") Long illustrationId,
            @Param("updateBy") String updateBy);

    List<TlIllustration> selectIllustrationList(
            @Param("worldlineId") Long worldlineId, @Param("adoptedOnly") Boolean adoptedOnly);

    TlIllustration selectIllustrationById(Long illustrationId);

    int insertIllustration(TlIllustration illustration);

    int updateIllustration(TlIllustration illustration);

    int deleteIllustrationById(Long illustrationId);

    int updateIllustrationStatus(
            @Param("illustrationId") Long illustrationId,
            @Param("status") String status,
            @Param("updateBy") String updateBy);

    List<TlAiTask> selectAiTaskList(TlAiTask task);

    List<TlAiTask> selectRecentAiTask(@Param("createBy") String createBy);

    TlAiTask selectAiTaskById(Long taskId);

    int insertAiTask(TlAiTask task);

    int updateAiTask(TlAiTask task);

    int deleteAiTaskByIds(Long[] taskIds);

    List<TlEventRelation> selectEventRelationList(@Param("worldlineId") Long worldlineId);

    int insertEventRelation(TlEventRelation relation);

    int deleteEventRelationById(Long relationId);

    TlUserProfile selectUserProfileByUserId(Long userId);

    TlUserProfile selectUserProfileByProfileId(Long profileId);

    int insertUserProfile(TlUserProfile profile);

    int updateUserProfile(TlUserProfile profile);

    Map<String, Object> selectPublicCreator(Long userId);
}
