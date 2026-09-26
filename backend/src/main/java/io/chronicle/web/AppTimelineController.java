package io.chronicle.web;

import io.chronicle.platform.ApiController;
import io.chronicle.platform.ApiResponse;
import io.chronicle.platform.ServiceException;
import io.chronicle.platform.Text;
import io.chronicle.storage.ImageTypes;
import io.chronicle.storage.ImageUploads;
import io.chronicle.storage.StoragePaths;
import io.chronicle.timeline.domain.TlAiTask;
import io.chronicle.timeline.domain.TlChapter;
import io.chronicle.timeline.domain.TlCharacter;
import io.chronicle.timeline.domain.TlEvent;
import io.chronicle.timeline.domain.TlEventRelation;
import io.chronicle.timeline.domain.TlIllustration;
import io.chronicle.timeline.domain.TlNationState;
import io.chronicle.timeline.domain.TlScreenshot;
import io.chronicle.timeline.domain.TlStage;
import io.chronicle.timeline.domain.TlWorldline;
import io.chronicle.timeline.domain.dto.ChatAskRequest;
import io.chronicle.timeline.domain.dto.IllustrationGenerateRequest;
import io.chronicle.timeline.service.ITimelineService;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app")
public class AppTimelineController extends ApiController {
    private final ITimelineService timelineService;
    private final ImageUploads imageUploads;

    public AppTimelineController(ITimelineService timelineService, ImageUploads imageUploads) {
        this.timelineService = timelineService;
        this.imageUploads = imageUploads;
    }

    @GetMapping("/worldline/myList")
    public ApiResponse myWorldlines() {
        return success(timelineService.selectMyWorldlineList(getUsername()));
    }

    @GetMapping("/worldline/{worldlineId}")
    public ApiResponse worldlineInfo(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        return success(timelineService.selectWorldlineById(worldlineId));
    }

    @PostMapping("/worldline")
    public ApiResponse addWorldline(@RequestBody TlWorldline worldline) {
        worldline.setCreateBy(getUsername());
        timelineService.insertWorldline(worldline);
        return success(worldline);
    }

    @PutMapping("/worldline")
    public ApiResponse editWorldline(@RequestBody TlWorldline worldline) {
        timelineService.checkWorldlineOwner(worldline.getWorldlineId(), getUsername());
        worldline.setUpdateBy(getUsername());
        return toAjax(timelineService.updateWorldline(worldline));
    }

    @DeleteMapping("/worldline/{worldlineId}")
    public ApiResponse deleteWorldline(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        return toAjax(timelineService.deleteWorldlineById(worldlineId));
    }

    @PostMapping("/worldline/uploadCover")
    public ApiResponse uploadCover(
            @RequestParam("worldlineId") Long worldlineId, @RequestParam("file") MultipartFile file)
            throws Exception {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        String path =
                imageUploads.upload(
                        StoragePaths.getProfile() + "/timeline/worldline/" + worldlineId + "/cover",
                        file,
                        ImageTypes.IMAGE_EXTENSION,
                        true);
        timelineService.updateWorldlineCover(worldlineId, path, getUsername());
        return success(path);
    }

    @PostMapping("/worldline/setVisibility")
    public ApiResponse setVisibility(@RequestBody Map<String, Object> body) {
        Long worldlineId = toLong(body.get("worldlineId"));
        String visibility = String.valueOf(body.getOrDefault("visibility", "0"));
        return toAjax(
                timelineService.updateWorldlineVisibility(worldlineId, visibility, getUsername()));
    }

    @GetMapping("/worldline/stat/{worldlineId}")
    public ApiResponse worldlineStat(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        return success(timelineService.selectWorldlineStat(worldlineId));
    }

    @GetMapping("/screenshot/list/{worldlineId}")
    public ApiResponse screenshotList(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        return success(timelineService.selectScreenshotList(worldlineId));
    }

    @PostMapping("/screenshot/upload")
    public ApiResponse uploadScreenshot(
            @RequestParam("worldlineId") Long worldlineId, @RequestParam("file") MultipartFile file)
            throws Exception {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        String path =
                imageUploads.upload(
                        StoragePaths.getProfile()
                                + "/timeline/worldline/"
                                + worldlineId
                                + "/screenshot",
                        file,
                        ImageTypes.IMAGE_EXTENSION,
                        true);
        TlScreenshot screenshot = new TlScreenshot();
        screenshot.setWorldlineId(worldlineId);
        screenshot.setImageUrl(path);
        screenshot.setOriginalName("uploaded-image");
        screenshot.setAiStatus("0");
        screenshot.setCreateBy(getUsername());
        timelineService.insertScreenshot(screenshot);
        return success(screenshot);
    }

    @PostMapping("/screenshot/recognize/{screenshotId}")
    public ApiResponse recognizeScreenshot(@PathVariable Long screenshotId) {
        return success(timelineService.recognizeScreenshot(screenshotId, getUsername()));
    }

    @GetMapping("/screenshot/draft/{screenshotId}")
    public ApiResponse screenshotDraft(@PathVariable Long screenshotId) {
        TlScreenshot screenshot = timelineService.selectScreenshotById(screenshotId);
        if (screenshot == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(screenshot.getWorldlineId(), getUsername());
        return success(screenshot);
    }

    @PostMapping("/screenshot/confirmDraft")
    public ApiResponse confirmDraft(@RequestBody Map<String, Object> body) {
        return success(
                timelineService.confirmScreenshotDraft(
                        toLong(body.get("screenshotId")), getUsername()));
    }

    @PostMapping("/screenshot/rejectDraft")
    public ApiResponse rejectDraft(@RequestBody Map<String, Object> body) {
        return toAjax(
                timelineService.rejectScreenshotDraft(
                        toLong(body.get("screenshotId")), getUsername()));
    }

    @DeleteMapping("/screenshot/{screenshotId}")
    public ApiResponse deleteScreenshot(@PathVariable Long screenshotId) {
        TlScreenshot screenshot = timelineService.selectScreenshotById(screenshotId);
        if (screenshot == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(screenshot.getWorldlineId(), getUsername());
        return toAjax(timelineService.deleteScreenshotById(screenshotId));
    }

    @GetMapping({"/event/list/{worldlineId}", "/event/timeline/{worldlineId}"})
    public ApiResponse eventList(@PathVariable Long worldlineId, TlEvent query) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        query.setWorldlineId(worldlineId);
        return success(timelineService.selectEventList(query));
    }

    @GetMapping("/event/{eventId}")
    public ApiResponse eventInfo(@PathVariable Long eventId) {
        TlEvent event = timelineService.selectEventById(eventId);
        if (event == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(event.getWorldlineId(), getUsername());
        return success(event);
    }

    @PostMapping("/event")
    public ApiResponse addEvent(@RequestBody TlEvent event) {
        timelineService.checkWorldlineOwner(event.getWorldlineId(), getUsername());
        event.setCreateBy(getUsername());
        timelineService.insertEvent(event);
        return success(event);
    }

    @PutMapping("/event")
    public ApiResponse editEvent(@RequestBody TlEvent event) {
        TlEvent saved = timelineService.selectEventById(event.getEventId());
        if (saved == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(saved.getWorldlineId(), getUsername());
        event.setWorldlineId(saved.getWorldlineId());
        event.setUpdateBy(getUsername());
        return toAjax(timelineService.updateEvent(event));
    }

    @DeleteMapping("/event/{eventId}")
    public ApiResponse deleteEvent(@PathVariable Long eventId) {
        TlEvent saved = timelineService.selectEventById(eventId);
        if (saved == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(saved.getWorldlineId(), getUsername());
        return toAjax(timelineService.deleteEventById(eventId));
    }

    @PostMapping("/event/generateFragment/{eventId}")
    public ApiResponse generateFragment(@PathVariable Long eventId) {
        return success(timelineService.generateEventFragment(eventId, getUsername()));
    }

    @PostMapping("/event/markDivergence/{eventId}")
    public ApiResponse markDivergence(@PathVariable Long eventId) {
        return toAjax(timelineService.markEventDivergence(eventId, getUsername()));
    }

    @PostMapping("/event/relation")
    public ApiResponse addRelation(@RequestBody TlEventRelation relation) {
        timelineService.checkWorldlineOwner(relation.getWorldlineId(), getUsername());
        relation.setCreateBy(getUsername());
        return toAjax(timelineService.insertEventRelation(relation));
    }

    @GetMapping("/event/relation/{worldlineId}")
    public ApiResponse relationList(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        return success(timelineService.selectEventRelationList(worldlineId));
    }

    @GetMapping("/stage/list/{worldlineId}")
    public ApiResponse stageList(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        return success(timelineService.selectStageList(worldlineId));
    }

    @GetMapping("/stage/{stageId}")
    public ApiResponse stageInfo(@PathVariable Long stageId) {
        TlStage stage = timelineService.selectStageById(stageId);
        if (stage == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(stage.getWorldlineId(), getUsername());
        return success(stage);
    }

    @PostMapping("/stage")
    public ApiResponse addStage(@RequestBody TlStage stage) {
        timelineService.checkWorldlineOwner(stage.getWorldlineId(), getUsername());
        stage.setCreateBy(getUsername());
        timelineService.insertStage(stage);
        return success(stage);
    }

    @PutMapping("/stage")
    public ApiResponse editStage(@RequestBody TlStage stage) {
        TlStage saved = timelineService.selectStageById(stage.getStageId());
        if (saved == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(saved.getWorldlineId(), getUsername());
        stage.setWorldlineId(saved.getWorldlineId());
        stage.setUpdateBy(getUsername());
        return toAjax(timelineService.updateStage(stage));
    }

    @DeleteMapping("/stage/{stageId}")
    public ApiResponse deleteStage(@PathVariable Long stageId) {
        TlStage saved = timelineService.selectStageById(stageId);
        if (saved == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(saved.getWorldlineId(), getUsername());
        return toAjax(timelineService.deleteStageById(stageId));
    }

    @PostMapping("/stage/autoSplit/{worldlineId}")
    public ApiResponse autoSplit(@PathVariable Long worldlineId) {
        return success(timelineService.autoSplitStages(worldlineId, getUsername()));
    }

    @PostMapping("/stage/bindEvents")
    public ApiResponse bindEvents(@RequestBody Map<String, Object> body) {
        return toAjax(
                timelineService.bindEventsToStage(
                        toLong(body.get("stageId")),
                        toLongArray(body.get("eventIds")),
                        getUsername()));
    }

    @PostMapping("/stage/generateSummary/{stageId}")
    public ApiResponse generateStageSummary(@PathVariable Long stageId) {
        TlStage stage = timelineService.selectStageById(stageId);
        if (stage == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(stage.getWorldlineId(), getUsername());
        TlEvent query = new TlEvent();
        query.setWorldlineId(stage.getWorldlineId());
        query.setStageId(stageId);
        List<TlEvent> events = timelineService.selectEventList(query);
        List<String> summaries = new ArrayList<>();
        for (TlEvent event : events) {
            summaries.add(event.getEventYear() + "年：" + event.getEventTitle());
        }
        stage.setStageSummary(
                Text.isEmpty(stage.getStageSummary())
                        ? String.join("；", summaries)
                        : stage.getStageSummary());
        stage.setUpdateBy(getUsername());
        timelineService.updateStage(stage);
        return success(stage);
    }

    @PostMapping("/stage/generateNovel/{stageId}")
    public ApiResponse generateStageNovel(
            @PathVariable Long stageId, @RequestBody(required = false) Map<String, Object> body) {
        String style = body == null ? null : stringValue(body.get("writingStyle"));
        return success(timelineService.generateNovelByStage(stageId, style, getUsername()));
    }

    @GetMapping("/chapter/list/{worldlineId}")
    public ApiResponse chapterList(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        return success(timelineService.selectChapterList(worldlineId, false));
    }

    @GetMapping("/chapter/{chapterId}")
    public ApiResponse chapterInfo(@PathVariable Long chapterId) {
        TlChapter chapter = timelineService.selectChapterById(chapterId);
        if (chapter == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(chapter.getWorldlineId(), getUsername());
        return success(chapter);
    }

    @PostMapping("/chapter")
    public ApiResponse addChapter(@RequestBody TlChapter chapter) {
        timelineService.checkWorldlineOwner(chapter.getWorldlineId(), getUsername());
        chapter.setCreateBy(getUsername());
        timelineService.insertChapter(chapter);
        return success(chapter);
    }

    @PutMapping("/chapter")
    public ApiResponse editChapter(@RequestBody TlChapter chapter) {
        TlChapter saved = timelineService.selectChapterById(chapter.getChapterId());
        if (saved == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(saved.getWorldlineId(), getUsername());
        chapter.setWorldlineId(saved.getWorldlineId());
        chapter.setUpdateBy(getUsername());
        return toAjax(timelineService.updateChapter(chapter));
    }

    @DeleteMapping("/chapter/{chapterId}")
    public ApiResponse deleteChapter(@PathVariable Long chapterId) {
        TlChapter chapter = timelineService.selectChapterById(chapterId);
        if (chapter == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(chapter.getWorldlineId(), getUsername());
        return toAjax(timelineService.deleteChapterById(chapterId));
    }

    @PostMapping("/chapter/generate")
    public ApiResponse generateChapter(@RequestBody Map<String, Object> body) {
        return success(
                timelineService.generateNovelByStage(
                        toLong(body.get("stageId")),
                        stringValue(body.get("writingStyle")),
                        getUsername()));
    }

    @PostMapping("/chapter/generateByStage/{stageId}")
    public ApiResponse generateChapterByStage(
            @PathVariable Long stageId, @RequestBody(required = false) Map<String, Object> body) {
        String style = body == null ? null : stringValue(body.get("writingStyle"));
        return success(timelineService.generateNovelByStage(stageId, style, getUsername()));
    }

    @PostMapping("/chapter/setPublic")
    public ApiResponse setChapterPublic(@RequestBody Map<String, Object> body) {
        return toAjax(
                timelineService.setChapterPublic(
                        toLong(body.get("chapterId")),
                        Boolean.TRUE.equals(body.get("publish")),
                        getUsername()));
    }

    @PostMapping("/chapter/exportMarkdown/{chapterId}")
    public ApiResponse exportMarkdown(@PathVariable Long chapterId) {
        TlChapter chapter = timelineService.selectChapterById(chapterId);
        if (chapter == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(chapter.getWorldlineId(), getUsername());
        return success(timelineService.exportChapterMarkdown(chapterId));
    }

    @PostMapping("/chapter/exportWord/{chapterId}")
    public void exportWord(@PathVariable Long chapterId, HttpServletResponse response)
            throws java.io.IOException {
        TlChapter chapter = timelineService.selectChapterById(chapterId);
        if (chapter == null) throw new ServiceException("记录不存在", 404);
        if (chapter == null) {
            response.sendError(404, "章节不存在");
            return;
        }
        timelineService.checkWorldlineOwner(chapter.getWorldlineId(), getUsername());
        String title = chapter.getChapterTitle() == null ? "未命名章节" : chapter.getChapterTitle();
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename*=UTF-8''"
                        + URLEncoder.encode(title + ".docx", StandardCharsets.UTF_8));
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph heading = doc.createParagraph();
            heading.setStyle("Title");
            XWPFRun titleRun = heading.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            String content = chapter.getContent() == null ? "" : chapter.getContent();
            for (String line : content.split("\\R", -1)) {
                XWPFParagraph p = doc.createParagraph();
                p.createRun().setText(line);
            }
            doc.write(response.getOutputStream());
        }
    }

    @GetMapping("/illustration/list/{worldlineId}")
    public ApiResponse illustrationList(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        return success(timelineService.selectIllustrationList(worldlineId, false));
    }

    @GetMapping("/illustration/{illustrationId}")
    public ApiResponse illustrationInfo(@PathVariable Long illustrationId) {
        TlIllustration illustration = timelineService.selectIllustrationById(illustrationId);
        if (illustration == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(illustration.getWorldlineId(), getUsername());
        return success(illustration);
    }

    @PostMapping("/illustration/generatePrompt")
    public ApiResponse generateIllustrationPrompt(
            @RequestBody IllustrationGenerateRequest request) {
        return success(timelineService.generateIllustrationPrompt(request, getUsername()));
    }

    @PostMapping("/illustration/generateImage")
    public ApiResponse generateIllustrationImage(@RequestBody IllustrationGenerateRequest request) {
        return success(timelineService.generateIllustrationImage(request, getUsername()));
    }

    @PostMapping("/illustration/generateByChapter/{chapterId}")
    public ApiResponse generateIllustrationByChapter(
            @PathVariable Long chapterId, @RequestBody IllustrationGenerateRequest request) {
        TlChapter chapter = timelineService.selectChapterById(chapterId);
        if (chapter == null) throw new ServiceException("记录不存在", 404);
        request.setChapterId(chapterId);
        request.setWorldlineId(chapter.getWorldlineId());
        return success(timelineService.generateIllustrationPrompt(request, getUsername()));
    }

    @PostMapping("/illustration/adopt/{illustrationId}")
    public ApiResponse adoptIllustration(@PathVariable Long illustrationId) {
        return toAjax(timelineService.adoptIllustration(illustrationId, getUsername()));
    }

    @PostMapping("/illustration/setChapterCover")
    public ApiResponse setChapterCover(@RequestBody Map<String, Object> body) {
        return toAjax(
                timelineService.setChapterCover(
                        toLong(body.get("chapterId")),
                        toLong(body.get("illustrationId")),
                        getUsername()));
    }

    @PostMapping("/illustration/discard/{illustrationId}")
    public ApiResponse discardIllustration(@PathVariable Long illustrationId) {
        return toAjax(timelineService.discardIllustration(illustrationId, getUsername()));
    }

    @DeleteMapping("/illustration/{illustrationId}")
    public ApiResponse deleteIllustration(@PathVariable Long illustrationId) {
        TlIllustration illustration = timelineService.selectIllustrationById(illustrationId);
        if (illustration == null) throw new ServiceException("记录不存在", 404);
        timelineService.checkWorldlineOwner(illustration.getWorldlineId(), getUsername());
        return toAjax(timelineService.deleteIllustrationById(illustrationId));
    }

    @GetMapping("/nation/list/{worldlineId}")
    public ApiResponse nationList(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        return success(timelineService.selectNationStateList(worldlineId));
    }

    @PostMapping("/nation")
    public ApiResponse addNation(@RequestBody TlNationState state) {
        timelineService.checkWorldlineOwner(state.getWorldlineId(), getUsername());
        state.setCreateBy(getUsername());
        return toAjax(timelineService.insertNationState(state));
    }

    @PutMapping("/nation")
    public ApiResponse editNation(@RequestBody TlNationState state) {
        TlNationState saved = timelineService.selectNationStateById(state.getStateId());
        if (saved == null) throw new ServiceException("记录不存在", 404);
        if (saved == null) {
            throw new ServiceException("国家档案不存在");
        }
        timelineService.checkWorldlineOwner(saved.getWorldlineId(), getUsername());
        state.setWorldlineId(saved.getWorldlineId());
        state.setUpdateBy(getUsername());
        return toAjax(timelineService.updateNationState(state));
    }

    @DeleteMapping("/nation/{stateId}")
    public ApiResponse deleteNation(@PathVariable Long stateId) {
        TlNationState saved = timelineService.selectNationStateById(stateId);
        if (saved == null) throw new ServiceException("记录不存在", 404);
        if (saved == null) {
            throw new ServiceException("国家档案不存在");
        }
        timelineService.checkWorldlineOwner(saved.getWorldlineId(), getUsername());
        return toAjax(timelineService.deleteNationStateById(stateId));
    }

    @GetMapping("/character/list/{worldlineId}")
    public ApiResponse characterList(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        return success(timelineService.selectCharacterList(worldlineId));
    }

    @PostMapping("/character/uploadPortrait")
    public ApiResponse uploadCharacterPortrait(
            @RequestParam("worldlineId") Long worldlineId,
            @RequestParam(value = "characterName", required = false) String characterName,
            @RequestParam("file") MultipartFile file)
            throws Exception {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要上传的人设图片");
        }
        String trimmedName = characterName == null ? "" : characterName.trim();
        String normalizedName = Text.isEmpty(trimmedName) ? "主要人物参考" : trimmedName;
        if (normalizedName.length() > 100) {
            throw new ServiceException("人物名称不能超过 100 个字符");
        }
        String path =
                imageUploads.upload(
                        StoragePaths.getProfile()
                                + "/timeline/worldline/"
                                + worldlineId
                                + "/character",
                        file,
                        ImageTypes.IMAGE_EXTENSION,
                        true);
        TlCharacter character = new TlCharacter();
        character.setWorldlineId(worldlineId);
        character.setCharacterName(normalizedName);
        character.setCharacterType("portrait_reference");
        character.setPortraitUrl(path);
        character.setAiGenerated("0");
        character.setCreateBy(getUsername());
        timelineService.insertCharacter(character);
        return success(character);
    }

    @PostMapping("/character")
    public ApiResponse addCharacter(@RequestBody TlCharacter character) {
        timelineService.checkWorldlineOwner(character.getWorldlineId(), getUsername());
        character.setCreateBy(getUsername());
        return toAjax(timelineService.insertCharacter(character));
    }

    @PutMapping("/character")
    public ApiResponse editCharacter(@RequestBody TlCharacter character) {
        TlCharacter saved = timelineService.selectCharacterById(character.getCharacterId());
        if (saved == null) throw new ServiceException("记录不存在", 404);
        if (saved == null) {
            throw new ServiceException("人物档案不存在");
        }
        timelineService.checkWorldlineOwner(saved.getWorldlineId(), getUsername());
        character.setWorldlineId(saved.getWorldlineId());
        character.setUpdateBy(getUsername());
        return toAjax(timelineService.updateCharacter(character));
    }

    @DeleteMapping("/character/{characterId}")
    public ApiResponse deleteCharacter(@PathVariable Long characterId) {
        TlCharacter saved = timelineService.selectCharacterById(characterId);
        if (saved == null) throw new ServiceException("记录不存在", 404);
        if (saved == null) {
            throw new ServiceException("人物档案不存在");
        }
        timelineService.checkWorldlineOwner(saved.getWorldlineId(), getUsername());
        return toAjax(timelineService.deleteCharacterById(characterId));
    }

    @PostMapping("/chat/ask")
    public ApiResponse chatAsk(@RequestBody ChatAskRequest request) {
        return success(timelineService.askWorldline(request, getUsername()));
    }

    @GetMapping("/chat/history/{worldlineId}")
    public ApiResponse chatHistory(@PathVariable Long worldlineId) {
        timelineService.checkWorldlineOwner(worldlineId, getUsername());
        TlAiTask query = new TlAiTask();
        query.setWorldlineId(worldlineId);
        query.setTaskType("chat");
        query.setCreateBy(getUsername());
        return success(timelineService.selectAiTaskList(query));
    }

    @DeleteMapping("/chat/history/{recordId}")
    public ApiResponse deleteChat(@PathVariable Long recordId) {
        requireOwnedTask(recordId);
        return toAjax(timelineService.deleteAiTaskByIds(new Long[] {recordId}));
    }

    @GetMapping("/aiTask/status/{taskId}")
    public ApiResponse aiTaskStatus(@PathVariable Long taskId) {
        return success(requireOwnedTask(taskId));
    }

    @GetMapping("/aiTask/recent")
    public ApiResponse aiTaskRecent() {
        return success(timelineService.selectRecentAiTask(getUsername()));
    }

    @PostMapping("/aiTask/retry/{taskId}")
    public ApiResponse retryAiTask(@PathVariable Long taskId) {
        return toAjax(timelineService.retryAiTask(taskId, getUsername()));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Long[] toLongArray(Object value) {
        if (value == null) {
            return new Long[0];
        }
        if (value instanceof List<?> list) {
            Long[] ids = new Long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ids[i] = toLong(list.get(i));
            }
            return ids;
        }
        return new Long[] {toLong(value)};
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private TlAiTask requireOwnedTask(Long id) {
        TlAiTask task = timelineService.selectAiTaskById(id);
        if (task == null) throw new ServiceException("记录不存在", 404);
        if (task == null) throw new ServiceException("任务不存在", 404);
        timelineService.checkWorldlineOwner(task.getWorldlineId(), getUsername());
        return task;
    }
}
