package io.chronicle.timeline.service;

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
import io.chronicle.timeline.domain.dto.ChatAskRequest;
import io.chronicle.timeline.domain.dto.IllustrationGenerateRequest;

import java.util.List;
import java.util.Map;

public interface ITimelineService {
    List<TlWorldline> selectWorldlineList(TlWorldline worldline);

    List<TlWorldline> selectPublicWorldlineList(TlWorldline worldline);

    List<TlWorldline> selectMyWorldlineList(String username);

    TlWorldline selectWorldlineById(Long worldlineId);

    int insertWorldline(TlWorldline worldline);

    int updateWorldline(TlWorldline worldline);

    int deleteWorldlineByIds(Long[] worldlineIds);

    int deleteWorldlineById(Long worldlineId);

    void checkWorldlineOwner(Long worldlineId, String username);

    int updateWorldlineCover(Long worldlineId, String coverUrl, String username);

    int updateWorldlineVisibility(Long worldlineId, String visibility, String username);

    TimelineStat selectWorldlineStat(Long worldlineId);

    List<TlScreenshot> selectScreenshotList(Long worldlineId);

    TlScreenshot selectScreenshotById(Long screenshotId);

    int insertScreenshot(TlScreenshot screenshot);

    Map<String, Object> recognizeScreenshot(Long screenshotId, String username);

    TlEvent confirmScreenshotDraft(Long screenshotId, String username);

    int rejectScreenshotDraft(Long screenshotId, String username);

    int deleteScreenshotById(Long screenshotId);

    List<TlEvent> selectEventList(TlEvent event);

    TlEvent selectEventById(Long eventId);

    int insertEvent(TlEvent event);

    int updateEvent(TlEvent event);

    int deleteEventById(Long eventId);

    String generateEventFragment(Long eventId, String username);

    String generateEventFragmentForAdmin(Long eventId, String username);

    int markEventDivergence(Long eventId, String username);

    List<TlEventRelation> selectEventRelationList(Long worldlineId);

    int insertEventRelation(TlEventRelation relation);

    List<TlStage> selectStageList(Long worldlineId);

    TlStage selectStageById(Long stageId);

    int insertStage(TlStage stage);

    int updateStage(TlStage stage);

    int deleteStageById(Long stageId);

    int bindEventsToStage(Long stageId, Long[] eventIds, String username);

    List<TlStage> autoSplitStages(Long worldlineId, String username);

    TlChapter generateNovelByStage(Long stageId, String writingStyle, String username);

    List<TlNationState> selectNationStateList(Long worldlineId);

    TlNationState selectNationStateById(Long stateId);

    int insertNationState(TlNationState state);

    int updateNationState(TlNationState state);

    int deleteNationStateById(Long stateId);

    List<TlCharacter> selectCharacterList(Long worldlineId);

    TlCharacter selectCharacterById(Long characterId);

    int insertCharacter(TlCharacter character);

    int updateCharacter(TlCharacter character);

    int deleteCharacterById(Long characterId);

    List<TlChapter> selectChapterList(Long worldlineId, Boolean publicOnly);

    TlChapter selectChapterById(Long chapterId);

    int insertChapter(TlChapter chapter);

    int updateChapter(TlChapter chapter);

    int deleteChapterById(Long chapterId);

    int setChapterPublic(Long chapterId, boolean publish, String username);

    String exportChapterMarkdown(Long chapterId);

    List<TlIllustration> selectIllustrationList(Long worldlineId, Boolean adoptedOnly);

    TlIllustration selectIllustrationById(Long illustrationId);

    int insertIllustration(TlIllustration illustration);

    int updateIllustration(TlIllustration illustration);

    int deleteIllustrationById(Long illustrationId);

    TlIllustration generateIllustrationPrompt(IllustrationGenerateRequest request, String username);

    TlIllustration generateIllustrationImage(IllustrationGenerateRequest request, String username);

    int adoptIllustration(Long illustrationId, String username);

    int discardIllustration(Long illustrationId, String username);

    int setChapterCover(Long chapterId, Long illustrationId, String username);

    String askWorldline(ChatAskRequest request, String username);

    List<TlAiTask> selectAiTaskList(TlAiTask task);

    List<TlAiTask> selectRecentAiTask(String username);

    TlAiTask selectAiTaskById(Long taskId);

    int retryAiTask(Long taskId, String username);

    int deleteAiTaskByIds(Long[] taskIds);

    TlUserProfile selectUserProfileByUserId(Long userId);

    int saveUserProfile(TlUserProfile profile);

    Map<String, Object> selectPublicCreator(Long userId);
}
