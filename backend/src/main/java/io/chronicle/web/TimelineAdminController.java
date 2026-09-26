package io.chronicle.web;

import io.chronicle.platform.ApiController;
import io.chronicle.platform.ApiResponse;
import io.chronicle.platform.PageResult;
import io.chronicle.timeline.domain.TlAiTask;
import io.chronicle.timeline.domain.TlChapter;
import io.chronicle.timeline.domain.TlCharacter;
import io.chronicle.timeline.domain.TlEvent;
import io.chronicle.timeline.domain.TlIllustration;
import io.chronicle.timeline.domain.TlNationState;
import io.chronicle.timeline.domain.TlStage;
import io.chronicle.timeline.domain.TlWorldline;
import io.chronicle.timeline.service.ITimelineService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/timeline")
public class TimelineAdminController extends ApiController {
    private final ITimelineService timelineService;
    private final io.chronicle.timeline.ai.AiProperties aiProperties;
    private final io.chronicle.timeline.ai.TimelineAiClient aiClient;

    public TimelineAdminController(
            ITimelineService timelineService,
            io.chronicle.timeline.ai.AiProperties aiProperties,
            io.chronicle.timeline.ai.TimelineAiClient aiClient) {
        this.timelineService = timelineService;
        this.aiProperties = aiProperties;
        this.aiClient = aiClient;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/worldline/list")
    public PageResult worldlineList(TlWorldline worldline) {
        startPage();
        return getDataTable(timelineService.selectWorldlineList(worldline));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/worldline/{worldlineId}")
    public ApiResponse worldlineInfo(@PathVariable Long worldlineId) {
        return success(timelineService.selectWorldlineById(worldlineId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/worldline")
    public ApiResponse editWorldline(@RequestBody TlWorldline worldline) {
        worldline.setUpdateBy(getUsername());
        return toAjax(timelineService.updateWorldline(worldline));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/worldline/{worldlineIds}")
    public ApiResponse removeWorldline(@PathVariable Long[] worldlineIds) {
        return toAjax(timelineService.deleteWorldlineByIds(worldlineIds));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/worldline/stat/{worldlineId}")
    public ApiResponse stat(@PathVariable Long worldlineId) {
        return success(timelineService.selectWorldlineStat(worldlineId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/worldline/auditVisibility")
    public ApiResponse auditVisibility(@RequestBody Map<String, Object> body) {
        Long worldlineId = toLong(body.get("worldlineId"));
        String visibility = String.valueOf(body.getOrDefault("visibility", "0"));
        return toAjax(
                timelineService.updateWorldlineVisibility(worldlineId, visibility, getUsername()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/screenshot/list")
    public PageResult screenshotList(Long worldlineId) {
        startPage();
        return getDataTable(timelineService.selectScreenshotList(worldlineId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/screenshot/{screenshotId}")
    public ApiResponse screenshotInfo(@PathVariable Long screenshotId) {
        return success(timelineService.selectScreenshotById(screenshotId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/screenshot/{screenshotIds}")
    public ApiResponse removeScreenshot(@PathVariable Long[] screenshotIds) {
        int rows = 0;
        for (Long screenshotId : screenshotIds) {
            rows += timelineService.deleteScreenshotById(screenshotId);
        }
        return toAjax(rows);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/event/list")
    public PageResult eventList(TlEvent event) {
        startPage();
        return getDataTable(timelineService.selectEventList(event));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/event/{eventId}")
    public ApiResponse eventInfo(@PathVariable Long eventId) {
        return success(timelineService.selectEventById(eventId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/event")
    public ApiResponse addEvent(@RequestBody TlEvent event) {
        event.setCreateBy(getUsername());
        return toAjax(timelineService.insertEvent(event));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/event")
    public ApiResponse editEvent(@RequestBody TlEvent event) {
        event.setUpdateBy(getUsername());
        return toAjax(timelineService.updateEvent(event));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/event/{eventIds}")
    public ApiResponse removeEvent(@PathVariable Long[] eventIds) {
        int rows = 0;
        for (Long eventId : eventIds) {
            rows += timelineService.deleteEventById(eventId);
        }
        return toAjax(rows);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/event/fragment/{eventId}")
    public ApiResponse eventFragment(@PathVariable Long eventId) {
        return success(timelineService.generateEventFragmentForAdmin(eventId, getUsername()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/event/divergence/{eventId}")
    public ApiResponse eventDivergence(@PathVariable Long eventId) {
        TlEvent event = timelineService.selectEventById(eventId);
        if (event == null) {
            return error("事件不存在");
        }
        event.setDivergenceFlag("1");
        event.setUpdateBy(getUsername());
        return toAjax(timelineService.updateEvent(event));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stage/list")
    public PageResult stageList(Long worldlineId) {
        startPage();
        return getDataTable(timelineService.selectStageList(worldlineId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stage/{id}")
    public ApiResponse stageInfo(@PathVariable Long id) {
        return success(timelineService.selectStageById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/stage")
    public ApiResponse addStage(@RequestBody TlStage value) {
        value.setCreateBy(getUsername());
        return toAjax(timelineService.insertStage(value));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/stage")
    public ApiResponse editStage(@RequestBody TlStage value) {
        value.setUpdateBy(getUsername());
        return toAjax(timelineService.updateStage(value));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/stage/{ids}")
    public ApiResponse removeStage(@PathVariable Long[] ids) {
        int rows = 0;
        for (Long id : ids) rows += timelineService.deleteStageById(id);
        return toAjax(rows);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/nation/list")
    public PageResult nationList(Long worldlineId) {
        startPage();
        return getDataTable(timelineService.selectNationStateList(worldlineId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/nation/{id}")
    public ApiResponse nationInfo(@PathVariable Long id) {
        return success(timelineService.selectNationStateById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/nation")
    public ApiResponse addNation(@RequestBody TlNationState value) {
        value.setCreateBy(getUsername());
        return toAjax(timelineService.insertNationState(value));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/nation")
    public ApiResponse editNation(@RequestBody TlNationState value) {
        value.setUpdateBy(getUsername());
        return toAjax(timelineService.updateNationState(value));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/nation/{ids}")
    public ApiResponse removeNation(@PathVariable Long[] ids) {
        int rows = 0;
        for (Long id : ids) rows += timelineService.deleteNationStateById(id);
        return toAjax(rows);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/character/list")
    public PageResult characterList(Long worldlineId) {
        startPage();
        return getDataTable(timelineService.selectCharacterList(worldlineId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/character/{id}")
    public ApiResponse characterInfo(@PathVariable Long id) {
        return success(timelineService.selectCharacterById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/character")
    public ApiResponse addCharacter(@RequestBody TlCharacter value) {
        value.setCreateBy(getUsername());
        return toAjax(timelineService.insertCharacter(value));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/character")
    public ApiResponse editCharacter(@RequestBody TlCharacter value) {
        value.setUpdateBy(getUsername());
        return toAjax(timelineService.updateCharacter(value));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/character/{ids}")
    public ApiResponse removeCharacter(@PathVariable Long[] ids) {
        int rows = 0;
        for (Long id : ids) rows += timelineService.deleteCharacterById(id);
        return toAjax(rows);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/analysis/{worldlineId}")
    public ApiResponse analysis(@PathVariable Long worldlineId) {
        TlEvent q = new TlEvent();
        q.setWorldlineId(worldlineId);
        var events = timelineService.selectEventList(q);
        var relations = timelineService.selectEventRelationList(worldlineId);
        var nodes =
                events.stream()
                        .map(
                                e ->
                                        Map.of(
                                                "id",
                                                e.getEventId(),
                                                "name",
                                                e.getEventTitle(),
                                                "year",
                                                e.getEventYear() == null ? 0 : e.getEventYear(),
                                                "divergence",
                                                "1".equals(e.getDivergenceFlag())))
                        .toList();
        var edges =
                relations.stream()
                        .map(
                                e ->
                                        Map.of(
                                                "source",
                                                e.getSourceEventId(),
                                                "target",
                                                e.getTargetEventId(),
                                                "type",
                                                e.getRelationType(),
                                                "description",
                                                e.getRelationDesc() == null
                                                        ? ""
                                                        : e.getRelationDesc()))
                        .toList();
        int score =
                timelineService.selectWorldlineStat(worldlineId).getDivergenceScore() == null
                        ? 0
                        : timelineService.selectWorldlineStat(worldlineId).getDivergenceScore();
        return success(Map.of("nodes", nodes, "edges", edges, "divergenceScore", score));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/chapter/list")
    public PageResult chapterList(Long worldlineId, Boolean publicOnly) {
        startPage();
        return getDataTable(
                timelineService.selectChapterList(worldlineId, publicOnly != null && publicOnly));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/chapter/{chapterId}")
    public ApiResponse chapterInfo(@PathVariable Long chapterId) {
        return success(timelineService.selectChapterById(chapterId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/chapter")
    public ApiResponse addChapter(@RequestBody TlChapter chapter) {
        chapter.setCreateBy(getUsername());
        return toAjax(timelineService.insertChapter(chapter));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/chapter")
    public ApiResponse editChapter(@RequestBody TlChapter chapter) {
        chapter.setUpdateBy(getUsername());
        return toAjax(timelineService.updateChapter(chapter));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/chapter/{chapterIds}")
    public ApiResponse removeChapter(@PathVariable Long[] chapterIds) {
        int rows = 0;
        for (Long chapterId : chapterIds) {
            rows += timelineService.deleteChapterById(chapterId);
        }
        return toAjax(rows);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/chapter/public")
    public ApiResponse chapterPublic(@RequestBody Map<String, Object> body) {
        Long chapterId = toLong(body.get("chapterId"));
        boolean publish = Boolean.parseBoolean(String.valueOf(body.getOrDefault("publish", true)));
        TlChapter chapter = timelineService.selectChapterById(chapterId);
        if (chapter == null) {
            return error("章节不存在");
        }
        chapter.setStatus(publish ? "3" : "2");
        chapter.setUpdateBy(getUsername());
        return toAjax(timelineService.updateChapter(chapter));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/illustration/list")
    public PageResult illustrationList(Long worldlineId, Boolean adoptedOnly) {
        startPage();
        return getDataTable(
                timelineService.selectIllustrationList(
                        worldlineId, adoptedOnly != null && adoptedOnly));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/illustration/{illustrationId}")
    public ApiResponse illustrationInfo(@PathVariable Long illustrationId) {
        return success(timelineService.selectIllustrationById(illustrationId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/illustration")
    public ApiResponse addIllustration(@RequestBody TlIllustration illustration) {
        illustration.setCreateBy(getUsername());
        return toAjax(timelineService.insertIllustration(illustration));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/illustration")
    public ApiResponse editIllustration(@RequestBody TlIllustration illustration) {
        illustration.setUpdateBy(getUsername());
        return toAjax(timelineService.updateIllustration(illustration));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/illustration/{illustrationIds}")
    public ApiResponse removeIllustration(@PathVariable Long[] illustrationIds) {
        int rows = 0;
        for (Long illustrationId : illustrationIds) {
            rows += timelineService.deleteIllustrationById(illustrationId);
        }
        return toAjax(rows);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/illustration/adopt/{illustrationId}")
    public ApiResponse adoptIllustration(@PathVariable Long illustrationId) {
        TlIllustration illustration = timelineService.selectIllustrationById(illustrationId);
        if (illustration == null) {
            return error("插图不存在");
        }
        illustration.setGenerateStatus("4");
        illustration.setUpdateBy(getUsername());
        return toAjax(timelineService.updateIllustration(illustration));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/illustration/discard/{illustrationId}")
    public ApiResponse discardIllustration(@PathVariable Long illustrationId) {
        TlIllustration illustration = timelineService.selectIllustrationById(illustrationId);
        if (illustration == null) {
            return error("插图不存在");
        }
        illustration.setGenerateStatus("5");
        illustration.setUpdateBy(getUsername());
        return toAjax(timelineService.updateIllustration(illustration));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audit/worldline/list")
    public PageResult auditWorldlineList(TlWorldline worldline) {
        startPage();
        return getDataTable(timelineService.selectWorldlineList(worldline));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audit/chapter/list")
    public PageResult auditChapterList(Long worldlineId) {
        startPage();
        return getDataTable(timelineService.selectChapterList(worldlineId, false));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audit/illustration/list")
    public PageResult auditIllustrationList(Long worldlineId) {
        startPage();
        return getDataTable(timelineService.selectIllustrationList(worldlineId, false));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/audit/pass")
    public ApiResponse auditPass(@RequestBody Map<String, Object> body) {
        String targetType = String.valueOf(body.get("targetType"));
        Long targetId = toLong(body.get("targetId"));
        if ("worldline".equals(targetType)) {
            return toAjax(timelineService.updateWorldlineVisibility(targetId, "1", getUsername()));
        }
        if ("chapter".equals(targetType)) {
            return toAjax(timelineService.setChapterPublic(targetId, true, getUsername()));
        }
        if ("illustration".equals(targetType)) {
            return toAjax(timelineService.adoptIllustration(targetId, getUsername()));
        }
        return error("未知审核对象");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/audit/reject")
    public ApiResponse auditReject(@RequestBody Map<String, Object> body) {
        String targetType = String.valueOf(body.get("targetType"));
        Long targetId = toLong(body.get("targetId"));
        if ("worldline".equals(targetType)) {
            return toAjax(timelineService.updateWorldlineVisibility(targetId, "0", getUsername()));
        }
        if ("chapter".equals(targetType)) {
            return toAjax(timelineService.setChapterPublic(targetId, false, getUsername()));
        }
        if ("illustration".equals(targetType)) {
            return toAjax(timelineService.discardIllustration(targetId, getUsername()));
        }
        return error("未知审核对象");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/audit/offShelf")
    public ApiResponse offShelf(@RequestBody Map<String, Object> body) {
        return auditReject(body);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/aiTask/list")
    public PageResult aiTaskList(TlAiTask task) {
        startPage();
        return getDataTable(timelineService.selectAiTaskList(task));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/aiTask/{taskId}")
    public ApiResponse aiTaskInfo(@PathVariable Long taskId) {
        return success(timelineService.selectAiTaskById(taskId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/aiTask/status/{taskId}")
    public ApiResponse aiTaskStatus(@PathVariable Long taskId) {
        return success(timelineService.selectAiTaskById(taskId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/aiTask/retry/{taskId}")
    public ApiResponse retryAiTask(@PathVariable Long taskId) {
        return toAjax(timelineService.retryAiTask(taskId, getUsername()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/aiTask/{taskIds}")
    public ApiResponse removeAiTask(@PathVariable Long[] taskIds) {
        return toAjax(timelineService.deleteAiTaskByIds(taskIds));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/aiConfig/list")
    public ApiResponse aiConfigList() {
        return success(
                Map.of(
                        "enabled", aiProperties.isEnabled(),
                        "visionProvider", "siliconflow",
                        "visionModel", aiProperties.getSiliconflow().getVisionModel(),
                        "textProvider", "siliconflow",
                        "textModel", aiProperties.getSiliconflow().getTextModel(),
                        "imageProvider", "openai-compatible",
                        "imageModel", aiProperties.getOpenai().getImageModel(),
                        "textConfigured", !aiProperties.getSiliconflow().getApiKey().isBlank(),
                        "imageConfigured", !aiProperties.getOpenai().getApiKey().isBlank(),
                        "configurationMode", "environment"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/aiConfig/test")
    public ApiResponse aiConfigTest() {
        aiClient.chat("Reply with the single word OK.");
        return success("文本服务连接成功");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"/aiConfig", "/aiConfig/testOnly"})
    public ApiResponse aiConfigAdd() {
        throw new io.chronicle.platform.ServiceException("AI 配置由部署环境变量管理，请更新环境变量后重启服务", 405);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/aiConfig")
    public ApiResponse aiConfigEdit() {
        throw new io.chronicle.platform.ServiceException("AI 配置由部署环境变量管理，请更新环境变量后重启服务", 405);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/aiConfig/{configIds}")
    public ApiResponse aiConfigRemove(@PathVariable Long[] configIds) {
        throw new io.chronicle.platform.ServiceException("AI 配置由部署环境变量管理，请更新环境变量后重启服务", 405);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
