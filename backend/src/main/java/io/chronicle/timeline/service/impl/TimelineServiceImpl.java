package io.chronicle.timeline.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import io.chronicle.auth.CurrentUser;
import io.chronicle.platform.ServiceException;
import io.chronicle.platform.Text;
import io.chronicle.timeline.ai.AiJsonParser;
import io.chronicle.timeline.ai.TimelineAiClient;
import io.chronicle.timeline.ai.TimelinePromptBuilder;
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
import io.chronicle.timeline.mapper.TimelineMapper;
import io.chronicle.timeline.service.ITimelineService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimelineServiceImpl implements ITimelineService {
    private final TimelineMapper timelineMapper;
    private final org.springframework.jdbc.core.JdbcTemplate db;
    private final io.chronicle.storage.MediaCatalog media;
    private final TimelineAiClient aiClient;
    private final TransactionTemplate transactionTemplate;

    public TimelineServiceImpl(
            TimelineMapper timelineMapper,
            TimelineAiClient aiClient,
            PlatformTransactionManager transactionManager,
            org.springframework.jdbc.core.JdbcTemplate db,
            io.chronicle.storage.MediaCatalog media) {
        this.timelineMapper = timelineMapper;
        this.db = db;
        this.media = media;
        this.aiClient = aiClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public List<TlWorldline> selectWorldlineList(TlWorldline worldline) {
        return timelineMapper.selectWorldlineList(worldline);
    }

    @Override
    public List<TlWorldline> selectPublicWorldlineList(TlWorldline worldline) {
        return timelineMapper.selectPublicWorldlineList(worldline);
    }

    @Override
    public List<TlWorldline> selectMyWorldlineList(String username) {
        return timelineMapper.selectMyWorldlineList(username);
    }

    @Override
    public TlWorldline selectWorldlineById(Long worldlineId) {
        return timelineMapper.selectWorldlineById(worldlineId);
    }

    @Override
    public int insertWorldline(TlWorldline worldline) {
        media.validate(worldline.getCoverUrl());
        if (Text.isEmpty(worldline.getVisibility())) {
            worldline.setVisibility("0");
        }
        if (worldline.getDivergenceScore() == null) {
            worldline.setDivergenceScore(0);
        }
        if (Text.isEmpty(worldline.getGameName())) {
            worldline.setGameName("维多利亚3");
        }
        return timelineMapper.insertWorldline(worldline);
    }

    @Override
    public int updateWorldline(TlWorldline worldline) {
        media.validate(worldline.getCoverUrl());
        return timelineMapper.updateWorldline(worldline);
    }

    @Override
    public int deleteWorldlineByIds(Long[] worldlineIds) {
        int rows = 0;
        for (Long id : worldlineIds) rows += deleteWorldlineById(id);
        return rows;
    }

    @Override
    public int deleteWorldlineById(Long worldlineId) {
        return transactionTemplate.execute(
                status -> {
                    for (String table :
                            java.util.List.of(
                                    "tl_event_relation",
                                    "tl_ai_task",
                                    "tl_illustration",
                                    "tl_chapter",
                                    "tl_event",
                                    "tl_screenshot",
                                    "tl_stage",
                                    "tl_character",
                                    "tl_nation_state"))
                        db.update("delete from " + table + " where worldline_id=?", worldlineId);
                    db.update(
                            "delete from tl_public_action where target_type='worldline' and"
                                    + " target_id=?",
                            worldlineId);
                    return timelineMapper.deleteWorldlineById(worldlineId);
                });
    }

    @Override
    public void checkWorldlineOwner(Long worldlineId, String username) {
        TlWorldline worldline = requireWorldline(worldlineId);
        if (!CurrentUser.isAdmin() && !username.equals(worldline.getCreateBy())) {
            throw new ServiceException("只能操作自己的世界线", 403);
        }
    }

    @Override
    public int updateWorldlineCover(Long worldlineId, String coverUrl, String username) {
        checkWorldlineOwner(worldlineId, username);
        return timelineMapper.updateWorldlineCover(worldlineId, coverUrl, username);
    }

    @Override
    public int updateWorldlineVisibility(Long worldlineId, String visibility, String username) {
        if (!java.util.List.of("0", "1").contains(visibility))
            throw new ServiceException("可见性值不正确");
        checkWorldlineOwner(worldlineId, username);
        return timelineMapper.updateWorldlineVisibility(worldlineId, visibility, username);
    }

    @Override
    public TimelineStat selectWorldlineStat(Long worldlineId) {
        TimelineStat stat = timelineMapper.selectWorldlineStat(worldlineId);
        if (stat != null) {
            stat.setDivergenceScore(recalculateDivergenceScore(worldlineId));
        }
        return stat;
    }

    @Override
    public List<TlScreenshot> selectScreenshotList(Long worldlineId) {
        return timelineMapper.selectScreenshotList(worldlineId);
    }

    @Override
    public TlScreenshot selectScreenshotById(Long screenshotId) {
        return timelineMapper.selectScreenshotById(screenshotId);
    }

    @Override
    public int insertScreenshot(TlScreenshot screenshot) {
        if (Text.isEmpty(screenshot.getAiStatus())) {
            screenshot.setAiStatus("0");
        }
        return timelineMapper.insertScreenshot(screenshot);
    }

    @Override
    public Map<String, Object> recognizeScreenshot(Long screenshotId, String username) {
        TlScreenshot screenshot = requireScreenshot(screenshotId);
        checkWorldlineOwner(screenshot.getWorldlineId(), username);
        TlWorldline worldline = requireWorldline(screenshot.getWorldlineId());

        String prompt =
                TimelinePromptBuilder.screenshotRecognition(worldline, screenshot.getImageUrl());
        TlAiTask task =
                createTask(
                        worldline.getWorldlineId(),
                        "event_recognition",
                        screenshotId,
                        screenshot.getImageUrl(),
                        prompt,
                        username);
        try {
            screenshot.setAiStatus("1");
            screenshot.setUpdateBy(username);
            timelineMapper.updateScreenshot(screenshot);

            String raw = aiClient.vision(prompt, screenshot.getImageUrl());
            JSONObject draft = AiJsonParser.extractJsonObject(raw);
            if (draft.isEmpty()) {
                throw new ServiceException("AI 未返回有效事件草稿，请重试", 502);
            }

            screenshot.setAiStatus("2");
            screenshot.setAiRawResult(raw);
            screenshot.setDraftEventJson(draft.toJSONString());
            screenshot.setUpdateBy(username);
            transactionTemplate.executeWithoutResult(
                    status -> {
                        timelineMapper.updateScreenshot(screenshot);
                        finishTask(task, raw);
                    });

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("task", task);
            result.put("draft", draft);
            return result;
        } catch (Exception e) {
            persistAiFailure(
                    task,
                    e,
                    () -> {
                        screenshot.setAiStatus("5");
                        screenshot.setUpdateBy(username);
                        timelineMapper.updateScreenshot(screenshot);
                    });
            throw e instanceof ServiceException
                    ? (ServiceException) e
                    : new ServiceException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public TlEvent confirmScreenshotDraft(Long screenshotId, String username) {
        TlScreenshot screenshot = requireScreenshot(screenshotId);
        checkWorldlineOwner(screenshot.getWorldlineId(), username);
        if (!"2".equals(screenshot.getAiStatus())) throw new ServiceException("只能确认待审核的截图草稿");
        JSONObject draft = AiJsonParser.extractJsonObject(screenshot.getDraftEventJson());
        if (draft.isEmpty()) {
            throw new ServiceException("当前截图没有可确认的事件草稿");
        }
        TlEvent event = new TlEvent();
        event.setWorldlineId(screenshot.getWorldlineId());
        event.setScreenshotId(screenshot.getScreenshotId());
        event.setEventTitle(defaultString(draft.getString("eventTitle"), "未命名事件"));
        event.setEventDate(draft.getString("eventDate"));
        event.setEventYear(draft.getInteger("eventYear"));
        event.setCountry(draft.getString("country"));
        event.setEventType(defaultString(draft.getString("eventType"), "other"));
        event.setRelatedForces(toCsv(draft.getJSONArray("relatedForces")));
        event.setImportanceLevel(defaultString(draft.getString("importanceLevel"), "1"));
        event.setDivergenceFlag(defaultString(draft.getString("divergenceFlag"), "0"));
        event.setSummary(draft.getString("summary"));
        event.setImpactAnalysis(draft.getString("impactAnalysis"));
        event.setNovelPotential(defaultString(draft.getString("novelPotential"), "0"));
        event.setRemark(draft.getString("remark"));
        event.setCreateBy(username);
        timelineMapper.insertEvent(event);

        screenshot.setAiStatus("3");
        screenshot.setUpdateBy(username);
        timelineMapper.updateScreenshot(screenshot);
        recalculateDivergenceScore(screenshot.getWorldlineId());
        return event;
    }

    @Override
    public int rejectScreenshotDraft(Long screenshotId, String username) {
        TlScreenshot screenshot = requireScreenshot(screenshotId);
        checkWorldlineOwner(screenshot.getWorldlineId(), username);
        screenshot.setAiStatus("4");
        screenshot.setUpdateBy(username);
        return timelineMapper.updateScreenshot(screenshot);
    }

    @Override
    @Transactional
    public int deleteScreenshotById(Long screenshotId) {
        db.update("update tl_event set screenshot_id=null where screenshot_id=?", screenshotId);
        return timelineMapper.deleteScreenshotById(screenshotId);
    }

    @Override
    public List<TlEvent> selectEventList(TlEvent event) {
        return timelineMapper.selectEventList(event);
    }

    @Override
    public TlEvent selectEventById(Long eventId) {
        return timelineMapper.selectEventById(eventId);
    }

    @Override
    public int insertEvent(TlEvent event) {
        validateEventLinks(event);
        normalizeEvent(event);
        int rows = timelineMapper.insertEvent(event);
        recalculateDivergenceScore(event.getWorldlineId());
        return rows;
    }

    @Override
    public int updateEvent(TlEvent event) {
        event.setWorldlineId(requireEvent(event.getEventId()).getWorldlineId());
        validateEventLinks(event);
        normalizeEvent(event);
        int rows = timelineMapper.updateEvent(event);
        TlEvent saved = timelineMapper.selectEventById(event.getEventId());
        if (saved != null) {
            recalculateDivergenceScore(saved.getWorldlineId());
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteEventById(Long eventId) {
        db.update(
                "delete from tl_event_relation where source_event_id=? or target_event_id=?",
                eventId,
                eventId);
        db.update("update tl_illustration set event_id=null where event_id=?", eventId);
        TlEvent saved = timelineMapper.selectEventById(eventId);
        int rows = timelineMapper.deleteEventById(eventId);
        if (saved != null) {
            recalculateDivergenceScore(saved.getWorldlineId());
        }
        return rows;
    }

    @Override
    public String generateEventFragment(Long eventId, String username) {
        return generateEventFragment(eventId, username, true);
    }

    @Override
    public String generateEventFragmentForAdmin(Long eventId, String username) {
        return generateEventFragment(eventId, username, false);
    }

    private String generateEventFragment(Long eventId, String username, boolean checkOwner) {
        TlEvent event = requireEvent(eventId);
        if (checkOwner) {
            checkWorldlineOwner(event.getWorldlineId(), username);
        }
        String prompt = TimelinePromptBuilder.eventFragment(event);
        TlAiTask task =
                createTask(
                        event.getWorldlineId(),
                        "novel_generate",
                        eventId,
                        JSON.toJSONString(event),
                        prompt,
                        username);
        try {
            String text = aiClient.chat(prompt);
            event.setAiDescription(text);
            event.setUpdateBy(username);
            transactionTemplate.executeWithoutResult(
                    status -> {
                        timelineMapper.updateEvent(event);
                        finishTask(task, text);
                    });
            return text;
        } catch (Exception e) {
            persistAiFailure(task, e, null);
            throw e instanceof ServiceException
                    ? (ServiceException) e
                    : new ServiceException(e.getMessage());
        }
    }

    @Override
    public int markEventDivergence(Long eventId, String username) {
        TlEvent event = requireEvent(eventId);
        checkWorldlineOwner(event.getWorldlineId(), username);
        int rows = timelineMapper.markEventDivergence(eventId, username);
        recalculateDivergenceScore(event.getWorldlineId());
        return rows;
    }

    @Override
    public List<TlEventRelation> selectEventRelationList(Long worldlineId) {
        return timelineMapper.selectEventRelationList(worldlineId);
    }

    @Override
    public int insertEventRelation(TlEventRelation relation) {
        sameWorldline(
                relation.getWorldlineId(),
                requireEvent(relation.getSourceEventId()).getWorldlineId());
        sameWorldline(
                relation.getWorldlineId(),
                requireEvent(relation.getTargetEventId()).getWorldlineId());
        if (Text.isEmpty(relation.getRelationType())) {
            relation.setRelationType("cause");
        }
        if (Text.isEmpty(relation.getAiGenerated())) {
            relation.setAiGenerated("0");
        }
        return timelineMapper.insertEventRelation(relation);
    }

    @Override
    public List<TlStage> selectStageList(Long worldlineId) {
        return timelineMapper.selectStageList(worldlineId);
    }

    @Override
    public TlStage selectStageById(Long stageId) {
        return timelineMapper.selectStageById(stageId);
    }

    @Override
    public int insertStage(TlStage stage) {
        if (Text.isEmpty(stage.getNovelStatus())) {
            stage.setNovelStatus("0");
        }
        if (Text.isEmpty(stage.getImageStatus())) {
            stage.setImageStatus("0");
        }
        return timelineMapper.insertStage(stage);
    }

    @Override
    public int updateStage(TlStage stage) {
        return timelineMapper.updateStage(stage);
    }

    @Override
    @Transactional
    public int deleteStageById(Long stageId) {
        db.update("update tl_event set stage_id=null where stage_id=?", stageId);
        db.update("update tl_chapter set stage_id=null where stage_id=?", stageId);
        db.update("update tl_illustration set stage_id=null where stage_id=?", stageId);
        return timelineMapper.deleteStageById(stageId);
    }

    @Override
    @Transactional
    public int bindEventsToStage(Long stageId, Long[] eventIds, String username) {
        TlStage stage = requireStage(stageId);
        checkWorldlineOwner(stage.getWorldlineId(), username);
        int rows = 0;
        if (eventIds != null) {
            for (Long eventId : eventIds) {
                sameWorldline(stage.getWorldlineId(), requireEvent(eventId).getWorldlineId());
                rows += timelineMapper.updateEventStage(eventId, stageId, username);
            }
        }
        return rows;
    }

    @Override
    public List<TlStage> autoSplitStages(Long worldlineId, String username) {
        checkWorldlineOwner(worldlineId, username);
        TlWorldline worldline = requireWorldline(worldlineId);
        TlEvent query = new TlEvent();
        query.setWorldlineId(worldlineId);
        List<TlEvent> events = timelineMapper.selectEventList(query);
        String prompt = TimelinePromptBuilder.stageSplit(worldline, events);
        TlAiTask task =
                createTask(
                        worldlineId,
                        "stage_split",
                        worldlineId,
                        JSON.toJSONString(events),
                        prompt,
                        username);
        try {
            String raw = aiClient.chat(prompt);
            JSONArray parsed = AiJsonParser.extractJsonArray(raw);
            if (parsed.isEmpty()) throw new ServiceException("AI 未返回有效阶段数据，请重试", 502);
            JSONArray stageDrafts = parsed;
            return transactionTemplate.execute(
                    status -> {
                        List<TlStage> stages = new ArrayList<>();
                        for (int i = 0; i < stageDrafts.size(); i++) {
                            JSONObject item = stageDrafts.getJSONObject(i);
                            TlStage stage = new TlStage();
                            stage.setWorldlineId(worldlineId);
                            stage.setStageName(defaultString(item.getString("stageName"), "未命名阶段"));
                            stage.setStartYear(item.getInteger("startYear"));
                            stage.setEndYear(item.getInteger("endYear"));
                            stage.setStageTheme(item.getString("stageTheme"));
                            stage.setStageSummary(item.getString("stageSummary"));
                            stage.setNovelStatus("0");
                            stage.setImageStatus("0");
                            stage.setCreateBy(username);
                            timelineMapper.insertStage(stage);
                            bindRelatedEvents(
                                    stage, item.getJSONArray("relatedEventIds"), events, username);
                            stages.add(stage);
                        }
                        finishTask(task, raw);
                        return stages;
                    });
        } catch (Exception e) {
            persistAiFailure(task, e, null);
            throw e instanceof ServiceException
                    ? (ServiceException) e
                    : new ServiceException(e.getMessage());
        }
    }

    @Override
    public TlChapter generateNovelByStage(Long stageId, String writingStyle, String username) {
        TlStage stage = requireStage(stageId);
        checkWorldlineOwner(stage.getWorldlineId(), username);
        TlWorldline worldline = requireWorldline(stage.getWorldlineId());
        TlEvent eventQuery = new TlEvent();
        eventQuery.setWorldlineId(stage.getWorldlineId());
        eventQuery.setStageId(stageId);
        List<TlEvent> events = timelineMapper.selectEventList(eventQuery);
        List<TlNationState> nations = timelineMapper.selectNationStateList(stage.getWorldlineId());
        List<TlCharacter> characters = timelineMapper.selectCharacterList(stage.getWorldlineId());
        String style = defaultString(writingStyle, worldline.getNarrativeStyle());
        String prompt =
                TimelinePromptBuilder.stageNovel(
                        worldline, stage, events, nations, characters, style);
        TlAiTask task =
                createTask(
                        stage.getWorldlineId(),
                        "novel_generate",
                        stageId,
                        JSON.toJSONString(events),
                        prompt,
                        username);
        try {
            stage.setNovelStatus("1");
            stage.setUpdateBy(username);
            timelineMapper.updateStage(stage);

            String raw = aiClient.chat(prompt);
            JSONObject json = AiJsonParser.extractJsonObject(raw);
            TlChapter chapter = new TlChapter();
            chapter.setWorldlineId(stage.getWorldlineId());
            chapter.setStageId(stageId);
            chapter.setChapterTitle(
                    defaultString(json.getString("chapterTitle"), stage.getStageName()));
            chapter.setChapterOrder(stage.getStartYear());
            chapter.setRelatedEventIds(joinEventIds(events));
            chapter.setWritingStyle(style);
            chapter.setContent(defaultString(json.getString("content"), raw));
            chapter.setAiPrompt(prompt);
            chapter.setStatus("2");
            chapter.setCreateBy(username);
            transactionTemplate.executeWithoutResult(
                    status -> {
                        timelineMapper.insertChapter(chapter);
                        stage.setNovelStatus("2");
                        stage.setUpdateBy(username);
                        timelineMapper.updateStage(stage);
                        finishTask(task, raw);
                    });
            return chapter;
        } catch (Exception e) {
            persistAiFailure(
                    task,
                    e,
                    () -> {
                        stage.setNovelStatus("3");
                        stage.setUpdateBy(username);
                        timelineMapper.updateStage(stage);
                    });
            throw e instanceof ServiceException
                    ? (ServiceException) e
                    : new ServiceException(e.getMessage());
        }
    }

    @Override
    public List<TlNationState> selectNationStateList(Long worldlineId) {
        return timelineMapper.selectNationStateList(worldlineId);
    }

    @Override
    public TlNationState selectNationStateById(Long stateId) {
        return timelineMapper.selectNationStateById(stateId);
    }

    @Override
    public int insertNationState(TlNationState state) {
        return timelineMapper.insertNationState(state);
    }

    @Override
    public int updateNationState(TlNationState state) {
        return timelineMapper.updateNationState(state);
    }

    @Override
    public int deleteNationStateById(Long stateId) {
        return timelineMapper.deleteNationStateById(stateId);
    }

    @Override
    public List<TlCharacter> selectCharacterList(Long worldlineId) {
        return timelineMapper.selectCharacterList(worldlineId);
    }

    @Override
    public TlCharacter selectCharacterById(Long characterId) {
        return timelineMapper.selectCharacterById(characterId);
    }

    @Override
    public int insertCharacter(TlCharacter character) {
        media.validate(character.getPortraitUrl());
        if (Text.isEmpty(character.getAiGenerated())) {
            character.setAiGenerated("0");
        }
        return timelineMapper.insertCharacter(character);
    }

    @Override
    public int updateCharacter(TlCharacter character) {
        media.validate(character.getPortraitUrl());
        return timelineMapper.updateCharacter(character);
    }

    @Override
    @Transactional
    public int deleteCharacterById(Long characterId) {
        db.update("update tl_illustration set character_id=null where character_id=?", characterId);
        return timelineMapper.deleteCharacterById(characterId);
    }

    @Override
    public List<TlChapter> selectChapterList(Long worldlineId, Boolean publicOnly) {
        List<TlChapter> chapters = timelineMapper.selectChapterList(worldlineId, publicOnly);
        if (publicOnly) chapters.forEach(c -> c.setAiPrompt(null));
        return chapters;
    }

    @Override
    public TlChapter selectChapterById(Long chapterId) {
        return timelineMapper.selectChapterById(chapterId);
    }

    @Override
    public int insertChapter(TlChapter chapter) {
        validateChapterLinks(chapter);
        if (chapter.getChapterOrder() == null) {
            chapter.setChapterOrder(0);
        }
        if (Text.isEmpty(chapter.getStatus())) {
            chapter.setStatus("0");
        }
        return timelineMapper.insertChapter(chapter);
    }

    @Override
    public int updateChapter(TlChapter chapter) {
        chapter.setWorldlineId(requireChapter(chapter.getChapterId()).getWorldlineId());
        validateChapterLinks(chapter);
        return timelineMapper.updateChapter(chapter);
    }

    @Override
    @Transactional
    public int deleteChapterById(Long chapterId) {
        db.update("update tl_illustration set chapter_id=null where chapter_id=?", chapterId);
        return timelineMapper.deleteChapterById(chapterId);
    }

    @Override
    public int setChapterPublic(Long chapterId, boolean publish, String username) {
        TlChapter chapter = requireChapter(chapterId);
        checkWorldlineOwner(chapter.getWorldlineId(), username);
        return timelineMapper.setChapterPublic(chapterId, publish ? "3" : "2", username);
    }

    @Override
    public String exportChapterMarkdown(Long chapterId) {
        TlChapter chapter = requireChapter(chapterId);
        TlWorldline worldline = requireWorldline(chapter.getWorldlineId());
        if (chapter.getRelatedEventIds() != null && !chapter.getRelatedEventIds().isBlank()) {
            for (String id : chapter.getRelatedEventIds().split(","))
                sameWorldline(
                        chapter.getWorldlineId(),
                        requireEvent(Long.valueOf(id.trim())).getWorldlineId());
        }
        return "# "
                + chapter.getChapterTitle()
                + "\n\n"
                + "> "
                + worldline.getWorldlineName()
                + "\n\n"
                + defaultString(chapter.getContent(), "");
    }

    @Override
    public List<TlIllustration> selectIllustrationList(Long worldlineId, Boolean adoptedOnly) {
        List<TlIllustration> pictures =
                timelineMapper.selectIllustrationList(worldlineId, adoptedOnly);
        if (adoptedOnly)
            pictures.forEach(
                    p -> {
                        p.setAiRawResult(null);
                        p.setPrompt(null);
                        p.setNegativePrompt(null);
                    });
        return pictures;
    }

    @Override
    public TlIllustration selectIllustrationById(Long illustrationId) {
        return timelineMapper.selectIllustrationById(illustrationId);
    }

    @Override
    public int insertIllustration(TlIllustration illustration) {
        media.validate(illustration.getImageUrl());
        if (Text.isEmpty(illustration.getGenerateStatus())) {
            illustration.setGenerateStatus("0");
        }
        return timelineMapper.insertIllustration(illustration);
    }

    @Override
    public int updateIllustration(TlIllustration illustration) {
        media.validate(illustration.getImageUrl());
        return timelineMapper.updateIllustration(illustration);
    }

    @Override
    @Transactional
    public int deleteIllustrationById(Long illustrationId) {
        db.update(
                "update tl_chapter set cover_illustration_id=null where cover_illustration_id=?",
                illustrationId);
        return timelineMapper.deleteIllustrationById(illustrationId);
    }

    @Override
    public TlIllustration generateIllustrationPrompt(
            IllustrationGenerateRequest request, String username) {
        checkWorldlineOwner(request.getWorldlineId(), username);
        validateIllustrationLinks(request);
        TlWorldline worldline = requireWorldline(request.getWorldlineId());
        TlEvent selectedEvent = requireIllustrationFragmentEvent(request);
        TlStage stage =
                request.getStageId() == null
                        ? (selectedEvent.getStageId() == null
                                ? null
                                : requireStage(selectedEvent.getStageId()))
                        : requireStage(request.getStageId());
        TlChapter chapter =
                request.getChapterId() == null ? null : requireChapter(request.getChapterId());
        List<TlEvent> events = new ArrayList<>();
        events.add(selectedEvent);
        List<TlCharacter> characters = timelineMapper.selectCharacterList(request.getWorldlineId());
        TlCharacter referenceCharacter = resolveReferenceCharacter(request);
        String prompt =
                TimelinePromptBuilder.illustrationPrompt(
                        worldline,
                        stage,
                        chapter == null ? "" : chapter.getContent(),
                        selectedEvent.getAiDescription(),
                        events,
                        characters,
                        referenceCharacter,
                        defaultString(request.getIllustrationType(), "event_scene"),
                        defaultString(request.getStyleType(), "victorian_illustration"));
        TlAiTask task =
                createTask(
                        request.getWorldlineId(),
                        "image_prompt",
                        request.getChapterId(),
                        JSON.toJSONString(request),
                        prompt,
                        username);
        try {
            String raw = aiClient.chat(prompt);
            JSONObject json = AiJsonParser.extractJsonObject(raw);
            TlIllustration illustration = new TlIllustration();
            illustration.setWorldlineId(request.getWorldlineId());
            illustration.setStageId(request.getStageId());
            illustration.setEventId(request.getEventId());
            illustration.setChapterId(request.getChapterId());
            illustration.setCharacterId(
                    referenceCharacter == null ? null : referenceCharacter.getCharacterId());
            illustration.setIllustrationType(
                    defaultString(request.getIllustrationType(), "event_scene"));
            illustration.setStyleType(
                    defaultString(request.getStyleType(), "victorian_illustration"));
            illustration.setIllustrationTitle(
                    defaultString(json.getString("illustrationTitle"), "未命名插图"));
            illustration.setSceneDescription(json.getString("sceneDescription"));
            illustration.setPrompt(defaultString(json.getString("prompt"), raw));
            illustration.setNegativePrompt(json.getString("negativePrompt"));
            illustration.setGenerateStatus("0");
            illustration.setAiRawResult(raw);
            illustration.setCreateBy(username);
            transactionTemplate.executeWithoutResult(
                    status -> {
                        timelineMapper.insertIllustration(illustration);
                        finishTask(task, raw);
                    });
            return illustration;
        } catch (Exception e) {
            persistAiFailure(task, e, null);
            throw e instanceof ServiceException
                    ? (ServiceException) e
                    : new ServiceException(e.getMessage());
        }
    }

    @Override
    public TlIllustration generateIllustrationImage(
            IllustrationGenerateRequest request, String username) {
        checkWorldlineOwner(request.getWorldlineId(), username);
        validateIllustrationLinks(request);
        TlEvent selectedEvent = requireIllustrationFragmentEvent(request);
        TlCharacter referenceCharacter = resolveReferenceCharacter(request);
        String referenceImageUrl =
                referenceCharacter == null ? null : referenceCharacter.getPortraitUrl();
        String prompt = enrichIllustrationImagePrompt(request.getPrompt(), referenceCharacter);
        String negativePrompt = request.getNegativePrompt();
        TlIllustration illustration = null;
        if (Text.isEmpty(prompt) && Text.isEmpty(request.getImageUrl())) {
            throw new ServiceException("请先生成或填写绘图提示词，再生成候选图");
        }
        TlAiTask task =
                createTask(
                        request.getWorldlineId(),
                        "image_generate",
                        request.getChapterId(),
                        JSON.toJSONString(request),
                        prompt,
                        username);
        try {
            String raw = aiClient.image(prompt, negativePrompt, referenceImageUrl);
            JSONObject json = AiJsonParser.extractJsonObject(raw);
            String imageUrl = defaultString(json.getString("imageUrl"), request.getImageUrl());
            media.validate(imageUrl);
            illustration = new TlIllustration();
            illustration.setWorldlineId(request.getWorldlineId());
            illustration.setStageId(request.getStageId());
            illustration.setEventId(request.getEventId());
            illustration.setChapterId(request.getChapterId());
            illustration.setCharacterId(
                    referenceCharacter == null ? null : referenceCharacter.getCharacterId());
            illustration.setIllustrationType(
                    defaultString(request.getIllustrationType(), "event_scene"));
            illustration.setStyleType(
                    defaultString(request.getStyleType(), "victorian_illustration"));
            illustration.setIllustrationTitle("AI候选插图");
            illustration.setPrompt(defaultString(json.getString("promptUsed"), prompt));
            illustration.setNegativePrompt(negativePrompt);
            illustration.setImageUrl(imageUrl);
            illustration.setGenerateStatus(Text.isEmpty(imageUrl) ? "3" : "2");
            illustration.setAiRawResult(raw);
            illustration.setSceneDescription(selectedEvent.getAiDescription());
            illustration.setCreateBy(username);
            TlIllustration generatedIllustration = illustration;
            transactionTemplate.executeWithoutResult(
                    status -> {
                        timelineMapper.insertIllustration(generatedIllustration);
                        finishTask(task, raw);
                    });
            return illustration;
        } catch (Exception e) {
            persistAiFailure(task, e, null);
            throw e instanceof ServiceException
                    ? (ServiceException) e
                    : new ServiceException(e.getMessage());
        }
    }

    private TlEvent requireIllustrationFragmentEvent(IllustrationGenerateRequest request) {
        if (request.getEventId() == null) {
            throw new ServiceException("请先选择要配图的小说片段");
        }
        TlEvent event = requireEvent(request.getEventId());
        if (!event.getWorldlineId().equals(request.getWorldlineId())) {
            throw new ServiceException("小说片段不属于当前世界线");
        }
        if (Text.isEmpty(event.getAiDescription())) {
            throw new ServiceException("请先在时间线为这个事件生成小说片段，再生成插图");
        }
        return event;
    }

    private TlCharacter resolveReferenceCharacter(IllustrationGenerateRequest request) {
        if (request.getCharacterId() == null) {
            if (!Text.isEmpty(request.getReferenceImageUrl())) {
                throw new ServiceException("人物参考图必须关联到当前世界线的人物档案");
            }
            return null;
        }
        TlCharacter character = timelineMapper.selectCharacterById(request.getCharacterId());
        if (character == null
                || character.getWorldlineId() == null
                || !character.getWorldlineId().equals(request.getWorldlineId())) {
            throw new ServiceException("人物档案不属于当前世界线");
        }
        if (Text.isEmpty(character.getPortraitUrl())) {
            throw new ServiceException("所选人物档案没有人设图，请重新上传或改用 AI 生成人物");
        }
        return character;
    }

    private String enrichIllustrationImagePrompt(String prompt, TlCharacter referenceCharacter) {
        if (Text.isEmpty(prompt)) {
            return "";
        }
        StringBuilder builder =
                new StringBuilder(prompt)
                        .append(
                                "\n\n"
                                        + "Safety and composition requirements: create a fictional,"
                                        + " non-graphic historical illustration. ")
                        .append(
                                "Show all people as adults and treat every person with dignity. Do"
                                        + " not include blood, visible injuries, ")
                        .append(
                                "sexual content, abusive labels, readable hateful slogans,"
                                    + " extremist symbols, or a recognizable living public figure."
                                    + " ")
                        .append(
                                "For war, enslavement, persecution, or political conflict, use an"
                                        + " indirect museum-documentary composition ")
                        .append(
                                "such as a council room, departure scene, public square, or"
                                        + " symbolic landscape rather than depicting abuse.");
        if (referenceCharacter != null) {
            builder.append(
                            "\n\n"
                                    + "The attached input image is an appearance reference for a"
                                    + " fictional adult character. ")
                    .append(
                            "Preserve the character's facial features, hairstyle, clothing design"
                                    + " and key identifying details. ")
                    .append(
                            "Use the requested historical scene for the new composition; do not"
                                    + " copy the reference image background or pose.");
        }
        return builder.toString();
    }

    @Override
    public int adoptIllustration(Long illustrationId, String username) {
        TlIllustration illustration = requireIllustration(illustrationId);
        checkWorldlineOwner(illustration.getWorldlineId(), username);
        return timelineMapper.updateIllustrationStatus(illustrationId, "4", username);
    }

    @Override
    public int discardIllustration(Long illustrationId, String username) {
        TlIllustration illustration = requireIllustration(illustrationId);
        checkWorldlineOwner(illustration.getWorldlineId(), username);
        return timelineMapper.updateIllustrationStatus(illustrationId, "5", username);
    }

    @Override
    public int setChapterCover(Long chapterId, Long illustrationId, String username) {
        TlChapter chapter = requireChapter(chapterId);
        checkWorldlineOwner(chapter.getWorldlineId(), username);
        sameWorldline(
                chapter.getWorldlineId(), requireIllustration(illustrationId).getWorldlineId());
        return timelineMapper.setChapterCover(chapterId, illustrationId, username);
    }

    @Override
    public String askWorldline(ChatAskRequest request, String username) {
        checkWorldlineOwner(request.getWorldlineId(), username);
        TlWorldline worldline = requireWorldline(request.getWorldlineId());
        TlEvent eventQuery = new TlEvent();
        eventQuery.setWorldlineId(request.getWorldlineId());
        List<TlEvent> events = timelineMapper.selectEventList(eventQuery);
        List<TlStage> stages = timelineMapper.selectStageList(request.getWorldlineId());
        List<TlNationState> nations =
                timelineMapper.selectNationStateList(request.getWorldlineId());
        List<TlCharacter> characters = timelineMapper.selectCharacterList(request.getWorldlineId());
        String prompt =
                TimelinePromptBuilder.worldlineChat(
                        worldline, events, stages, nations, characters, request.getQuestion());
        TlAiTask task =
                createTask(
                        request.getWorldlineId(),
                        "chat",
                        request.getWorldlineId(),
                        request.getQuestion(),
                        prompt,
                        username);
        try {
            String answer = aiClient.chat(prompt);
            finishTask(task, answer);
            return answer;
        } catch (Exception e) {
            failTask(task, e.getMessage());
            throw e instanceof ServiceException
                    ? (ServiceException) e
                    : new ServiceException(e.getMessage());
        }
    }

    @Override
    public List<TlAiTask> selectAiTaskList(TlAiTask task) {
        return timelineMapper.selectAiTaskList(task);
    }

    @Override
    public List<TlAiTask> selectRecentAiTask(String username) {
        return timelineMapper.selectRecentAiTask(username);
    }

    @Override
    public TlAiTask selectAiTaskById(Long taskId) {
        return timelineMapper.selectAiTaskById(taskId);
    }

    @Override
    public int retryAiTask(Long taskId, String username) {
        TlAiTask task = requireTask(taskId);
        checkWorldlineOwner(task.getWorldlineId(), username);
        if (!CurrentUser.isAdmin() && !username.equals(task.getCreateBy()))
            throw new ServiceException("只能重试自己的 AI 任务", 403);
        if (!"3".equals(task.getTaskStatus())) throw new ServiceException("只有失败任务可以重试");
        task.setTaskStatus("1");
        task.setErrorMsg("");
        timelineMapper.updateAiTask(task);
        try {
            switch (task.getTaskType()) {
                case "event_recognition" -> recognizeScreenshot(task.getRelatedId(), username);
                case "stage_split" -> autoSplitStages(task.getWorldlineId(), username);
                case "novel_generate" -> {
                    if (task.getInputContent() != null
                            && task.getInputContent().trim().startsWith("{"))
                        generateEventFragment(task.getRelatedId(), username);
                    else generateNovelByStage(task.getRelatedId(), null, username);
                }
                case "image_prompt" ->
                        generateIllustrationPrompt(
                                JSON.parseObject(
                                        task.getInputContent(), IllustrationGenerateRequest.class),
                                username);
                case "image_generate" ->
                        generateIllustrationImage(
                                JSON.parseObject(
                                        task.getInputContent(), IllustrationGenerateRequest.class),
                                username);
                case "chat" -> {
                    ChatAskRequest request = new ChatAskRequest();
                    request.setWorldlineId(task.getWorldlineId());
                    request.setQuestion(task.getInputContent());
                    askWorldline(request, username);
                }
                default -> throw new ServiceException("此任务类型不支持重试");
            }
            finishTask(task, "重试完成，结果保存在最新任务中");
            return 1;
        } catch (RuntimeException e) {
            failTask(task, e.getMessage());
            throw e;
        }
    }

    @Override
    public int deleteAiTaskByIds(Long[] taskIds) {
        return timelineMapper.deleteAiTaskByIds(taskIds);
    }

    @Override
    public TlUserProfile selectUserProfileByUserId(Long userId) {
        return timelineMapper.selectUserProfileByUserId(userId);
    }

    @Override
    public int saveUserProfile(TlUserProfile profile) {
        media.validate(profile.getAvatarUrl());
        media.validate(profile.getHomepageCoverUrl());
        TlUserProfile old = timelineMapper.selectUserProfileByUserId(profile.getUserId());
        if (old == null) {
            return timelineMapper.insertUserProfile(profile);
        }
        return timelineMapper.updateUserProfile(profile);
    }

    @Override
    public Map<String, Object> selectPublicCreator(Long userId) {
        Map<String, Object> creator = timelineMapper.selectPublicCreator(userId);
        if (creator == null) {
            throw new ServiceException("创作者不存在", 404);
        }
        String username =
                db.queryForObject(
                        "select user_name from app_user where user_id=?", String.class, userId);
        List<Map<String, Object>> published = new ArrayList<>();
        for (TlWorldline worldline : timelineMapper.selectMyWorldlineList(username)) {
            if (!"1".equals(worldline.getVisibility())) {
                continue;
            }
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("worldlineId", worldline.getWorldlineId());
            card.put("creatorId", userId);
            card.put("worldlineName", worldline.getWorldlineName());
            card.put("gameName", worldline.getGameName());
            card.put("mainCountry", worldline.getMainCountry());
            card.put("startYear", worldline.getStartYear());
            card.put("currentYear", worldline.getCurrentYear());
            card.put("narrativeStyle", worldline.getNarrativeStyle());
            card.put("description", worldline.getDescription());
            card.put("coverUrl", worldline.getCoverUrl());
            card.put("visibility", "1");
            published.add(card);
        }
        creator.put("worldlines", published);
        return creator;
    }

    private TlAiTask createTask(
            Long worldlineId,
            String taskType,
            Long relatedId,
            String input,
            String prompt,
            String username) {
        TlAiTask task = new TlAiTask();
        task.setWorldlineId(worldlineId);
        task.setTaskType(taskType);
        task.setRelatedId(relatedId);
        task.setInputContent(input);
        task.setPrompt(prompt);
        task.setTaskStatus("1");
        task.setCreateBy(username);
        timelineMapper.insertAiTask(task);
        return task;
    }

    private void finishTask(TlAiTask task, String result) {
        task.setTaskStatus("2");
        task.setResultContent(result);
        task.setFinishTime(new Date());
        timelineMapper.updateAiTask(task);
    }

    private void failTask(TlAiTask task, String error) {
        task.setTaskStatus("3");
        task.setErrorMsg(error);
        task.setFinishTime(new Date());
        timelineMapper.updateAiTask(task);
    }

    /**
     * Persist failure state in its own short transaction without replacing the original AI/provider
     * exception when the status update itself also fails.
     */
    private void persistAiFailure(
            TlAiTask task, Exception originalError, Runnable domainStatusUpdate) {
        try {
            transactionTemplate.executeWithoutResult(
                    status -> {
                        if (domainStatusUpdate != null) {
                            domainStatusUpdate.run();
                        }
                        failTask(task, originalError.getMessage());
                    });
        } catch (Exception persistenceError) {
            originalError.addSuppressed(persistenceError);
        }
    }

    private int recalculateDivergenceScore(Long worldlineId) {
        TlEvent query = new TlEvent();
        query.setWorldlineId(worldlineId);
        List<TlEvent> events = timelineMapper.selectEventList(query);
        int score = 0;
        for (TlEvent event : events) {
            if ("1".equals(event.getDivergenceFlag()) || "4".equals(event.getImportanceLevel())) {
                score += 15;
            } else if ("3".equals(event.getImportanceLevel())) {
                score += 8;
            } else if ("2".equals(event.getImportanceLevel())) {
                score += 3;
            }
        }
        score = Math.min(100, score);
        timelineMapper.updateWorldlineDivergence(worldlineId, score);
        return score;
    }

    private void normalizeEvent(TlEvent event) {
        if (Text.isEmpty(event.getEventType())) {
            event.setEventType("other");
        }
        if (Text.isEmpty(event.getImportanceLevel())) {
            event.setImportanceLevel("1");
        }
        if (Text.isEmpty(event.getDivergenceFlag())) {
            event.setDivergenceFlag("0");
        }
        if (Text.isEmpty(event.getNovelPotential())) {
            event.setNovelPotential("0");
        }
    }

    private void bindRelatedEvents(
            TlStage stage, JSONArray eventIds, List<TlEvent> events, String username) {
        if (eventIds != null && !eventIds.isEmpty()) {
            for (int i = 0; i < eventIds.size(); i++) {
                sameWorldline(
                        stage.getWorldlineId(), requireEvent(eventIds.getLong(i)).getWorldlineId());
                timelineMapper.updateEventStage(eventIds.getLong(i), stage.getStageId(), username);
            }
            return;
        }
        for (TlEvent event : events) {
            Integer year = event.getEventYear();
            if (year != null
                    && stage.getStartYear() != null
                    && stage.getEndYear() != null
                    && year >= stage.getStartYear()
                    && year <= stage.getEndYear()) {
                timelineMapper.updateEventStage(event.getEventId(), stage.getStageId(), username);
            }
        }
    }

    private String joinEventIds(List<TlEvent> events) {
        List<String> ids = new ArrayList<>();
        for (TlEvent event : events) {
            if (event.getEventId() != null) {
                ids.add(String.valueOf(event.getEventId()));
            }
        }
        return String.join(",", ids);
    }

    private String toCsv(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            values.add(array.getString(i));
        }
        return String.join(",", values);
    }

    private String defaultString(String value, String fallback) {
        return Text.isEmpty(value) ? fallback : value;
    }

    private TlWorldline requireWorldline(Long worldlineId) {
        TlWorldline worldline = timelineMapper.selectWorldlineById(worldlineId);
        if (worldline == null) {
            throw new ServiceException("世界线不存在");
        }
        return worldline;
    }

    private TlScreenshot requireScreenshot(Long screenshotId) {
        TlScreenshot screenshot = timelineMapper.selectScreenshotById(screenshotId);
        if (screenshot == null) {
            throw new ServiceException("截图不存在");
        }
        return screenshot;
    }

    private TlEvent requireEvent(Long eventId) {
        TlEvent event = timelineMapper.selectEventById(eventId);
        if (event == null) {
            throw new ServiceException("事件不存在");
        }
        return event;
    }

    private TlStage requireStage(Long stageId) {
        TlStage stage = timelineMapper.selectStageById(stageId);
        if (stage == null) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    private TlChapter requireChapter(Long chapterId) {
        TlChapter chapter = timelineMapper.selectChapterById(chapterId);
        if (chapter == null) {
            throw new ServiceException("章节不存在");
        }
        return chapter;
    }

    private TlIllustration requireIllustration(Long illustrationId) {
        TlIllustration illustration = timelineMapper.selectIllustrationById(illustrationId);
        if (illustration == null) {
            throw new ServiceException("插图不存在");
        }
        return illustration;
    }

    private TlAiTask requireTask(Long taskId) {
        TlAiTask task = timelineMapper.selectAiTaskById(taskId);
        if (task == null) {
            throw new ServiceException("AI任务不存在");
        }
        return task;
    }

    private void sameWorldline(Long expected, Long actual) {
        if (expected == null || !expected.equals(actual))
            throw new ServiceException("关联记录必须属于同一世界线", 403);
    }

    private void validateEventLinks(TlEvent event) {
        requireWorldline(event.getWorldlineId());
        if (event.getStageId() != null)
            sameWorldline(
                    event.getWorldlineId(), requireStage(event.getStageId()).getWorldlineId());
        if (event.getScreenshotId() != null)
            sameWorldline(
                    event.getWorldlineId(),
                    requireScreenshot(event.getScreenshotId()).getWorldlineId());
    }

    private void validateChapterLinks(TlChapter chapter) {
        requireWorldline(chapter.getWorldlineId());
        if (chapter.getRelatedEventIds() != null && !chapter.getRelatedEventIds().isBlank()) {
            for (String id : chapter.getRelatedEventIds().split(","))
                sameWorldline(
                        chapter.getWorldlineId(),
                        requireEvent(Long.valueOf(id.trim())).getWorldlineId());
        }
        if (chapter.getStageId() != null)
            sameWorldline(
                    chapter.getWorldlineId(), requireStage(chapter.getStageId()).getWorldlineId());
        if (chapter.getCoverIllustrationId() != null)
            sameWorldline(
                    chapter.getWorldlineId(),
                    requireIllustration(chapter.getCoverIllustrationId()).getWorldlineId());
    }

    private void validateIllustrationLinks(IllustrationGenerateRequest request) {
        if (request.getStageId() != null)
            sameWorldline(
                    request.getWorldlineId(), requireStage(request.getStageId()).getWorldlineId());
        if (request.getChapterId() != null)
            sameWorldline(
                    request.getWorldlineId(),
                    requireChapter(request.getChapterId()).getWorldlineId());
    }
}
