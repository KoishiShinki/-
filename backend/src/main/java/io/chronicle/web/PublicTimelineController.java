package io.chronicle.web;

import io.chronicle.platform.ApiController;
import io.chronicle.platform.ApiResponse;
import io.chronicle.platform.PageResult;
import io.chronicle.platform.ServiceException;
import io.chronicle.timeline.domain.TlChapter;
import io.chronicle.timeline.domain.TlEvent;
import io.chronicle.timeline.domain.TlWorldline;
import io.chronicle.timeline.service.ITimelineService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicTimelineController extends ApiController {
    private final ITimelineService timelineService;

    public PublicTimelineController(ITimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/worldline/list")
    public PageResult worldlineList(TlWorldline worldline) {
        startPage();
        return getDataTable(timelineService.selectPublicWorldlineList(worldline));
    }

    @GetMapping("/worldline/{worldlineId}")
    public ApiResponse worldlineInfo(@PathVariable Long worldlineId) {
        TlWorldline worldline = requirePublicWorldline(worldlineId);
        return success(worldline);
    }

    @GetMapping("/worldline/timeline/{worldlineId}")
    public ApiResponse worldlineTimeline(@PathVariable Long worldlineId) {
        requirePublicWorldline(worldlineId);
        TlEvent query = new TlEvent();
        query.setWorldlineId(worldlineId);
        return success(timelineService.selectEventList(query));
    }

    @GetMapping("/chapter/list/{worldlineId}")
    public ApiResponse chapterList(@PathVariable Long worldlineId) {
        requirePublicWorldline(worldlineId);
        return success(timelineService.selectChapterList(worldlineId, true));
    }

    @GetMapping("/chapter/{chapterId}")
    public ApiResponse chapterInfo(@PathVariable Long chapterId) {
        TlChapter chapter = timelineService.selectChapterById(chapterId);
        if (chapter == null || !"3".equals(chapter.getStatus())) {
            throw new ServiceException("章节未公开");
        }
        requirePublicWorldline(chapter.getWorldlineId());
        chapter.setAiPrompt(null);
        return success(chapter);
    }

    @GetMapping("/illustration/list/{worldlineId}")
    public ApiResponse illustrationList(@PathVariable Long worldlineId) {
        requirePublicWorldline(worldlineId);
        return success(timelineService.selectIllustrationList(worldlineId, true));
    }

    @GetMapping("/creator/{userId}")
    public ApiResponse creator(@PathVariable Long userId) {
        return success(timelineService.selectPublicCreator(userId));
    }

    @PostMapping("/action/view")
    public ApiResponse actionView(@RequestBody(required = false) Object body) {
        return success();
    }

    @PostMapping("/action/like")
    public ApiResponse actionLike(@RequestBody(required = false) Object body) {
        return success();
    }

    @PostMapping("/action/favorite")
    public ApiResponse actionFavorite(@RequestBody(required = false) Object body) {
        return success();
    }

    private TlWorldline requirePublicWorldline(Long worldlineId) {
        TlWorldline worldline = timelineService.selectWorldlineById(worldlineId);
        if (worldline == null || !"1".equals(worldline.getVisibility())) {
            throw new ServiceException("世界线未公开");
        }
        return worldline;
    }
}
