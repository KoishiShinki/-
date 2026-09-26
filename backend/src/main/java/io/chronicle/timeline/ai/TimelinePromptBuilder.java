package io.chronicle.timeline.ai;

import com.alibaba.fastjson2.JSON;

import io.chronicle.timeline.domain.TlCharacter;
import io.chronicle.timeline.domain.TlEvent;
import io.chronicle.timeline.domain.TlNationState;
import io.chronicle.timeline.domain.TlStage;
import io.chronicle.timeline.domain.TlWorldline;

import java.util.List;

public class TimelinePromptBuilder {
    private static final String SCREENSHOT_GAME_IDENTITY_RULE =
            """
            【最高优先级：游戏身份强制规则】
            1. 本系统中的待识别截图已由业务方确认来自《维多利亚3》（Victoria 3），必须始终按《维多利亚3》的游戏背景、界面和机制进行分析。
            2. 严禁将截图识别、猜测或描述为《钢铁雄心4》《钢铁雄心IV》、Hearts of Iron IV、Hearts of Iron 4、HOI4 或其他游戏。
            3. 即使地图、战争面板、国旗、字体或大战略游戏界面相似，也不能据此改变已经确认的游戏身份。
            4. 如果局部文字、日期、国家、事件或数值看不清，只能将对应信息标记为“不确定”或留空；不得用其他游戏的内容补全，不得猜测游戏名称。
            5. 最终输出的任何字段（包括标题、摘要、影响分析和备注）都不得出现《钢铁雄心》及其中文、英文或缩写名称，也不要讨论或质疑游戏身份。

            """;

    private TimelinePromptBuilder() {}

    public static String screenshotGameIdentityRule() {
        return SCREENSHOT_GAME_IDENTITY_RULE;
    }

    public static String screenshotRecognition(TlWorldline worldline, String imageUrl) {
        return SCREENSHOT_GAME_IDENTITY_RULE
                + """
                  你是一个大战略游戏历史档案分析助手。请根据用户上传的游戏截图和当前世界线设定，识别其中可能包含的历史事件信息。
                  不要输出游戏攻略，不要编造截图中完全不存在的信息。若图片无法直接访问，请结合世界线信息给出保守草稿并在 remark 中说明。
                  输出必须是 JSON，不要输出 Markdown，不要输出额外解释。

                  当前世界线：
                  %s

                  截图地址：
                  %s

                  返回格式：
                  {
                    "eventTitle": "",
                    "eventDate": "",
                    "eventYear": 0,
                    "country": "",
                    "eventType": "other",
                    "relatedForces": [],
                    "importanceLevel": "1",
                    "divergenceFlag": "0",
                    "summary": "",
                    "impactAnalysis": "",
                    "novelPotential": "0",
                    "remark": ""
                  }
                  """
                        .formatted(JSON.toJSONString(worldline), imageUrl);
    }

    public static String stageSplit(TlWorldline worldline, List<TlEvent> events) {
        return """
               你是一个架空历史编年史编辑。请根据世界线设定和事件列表，将事件划分为若干历史阶段。
               阶段数量不要过多，优先按重大转折点划分。输出 JSON 数组，不要输出 Markdown。

               世界线设定：
               %s

               事件列表：
               %s

               返回格式：
               [
                 {
                   "stageName": "",
                   "startYear": 1836,
                   "endYear": 1845,
                   "stageTheme": "",
                   "stageSummary": "",
                   "relatedEventIds": []
                 }
               ]
               """
                .formatted(JSON.toJSONString(worldline), JSON.toJSONString(events));
    }

    public static String stageNovel(
            TlWorldline worldline,
            TlStage stage,
            List<TlEvent> events,
            List<TlNationState> nations,
            List<TlCharacter> characters,
            String writingStyle) {
        return """
               你是一名架空历史小说作者。请根据世界线设定、历史阶段、事件列表、国家档案和人物档案，生成一章小说正文。
               要像真实历史正在发生，不要写成游戏攻略。保持事件逻辑和时间顺序一致。
               输出 JSON，不要输出 Markdown。必须包含 chapterTitle 和 content。

               世界线设定：
               %s

               历史阶段：
               %s

               事件列表：
               %s

               国家档案：
               %s

               人物档案：
               %s

               写作风格：%s

               返回格式：
               {
                 "chapterTitle": "",
                 "content": ""
               }
               """
                .formatted(
                        JSON.toJSONString(worldline),
                        JSON.toJSONString(stage),
                        JSON.toJSONString(events),
                        JSON.toJSONString(nations),
                        JSON.toJSONString(characters),
                        writingStyle);
    }

    public static String eventFragment(TlEvent event) {
        return """
               你是一名架空历史小说作者。请根据以下历史事件生成一段小说片段。
               只围绕该事件展开，不要改变事件结果，不要出现游戏界面、数值、按钮等内容。

               事件信息：
               %s
               """
                .formatted(JSON.toJSONString(event));
    }

    public static String illustrationPrompt(
            TlWorldline worldline,
            TlStage stage,
            String chapterContent,
            String fragmentContent,
            List<TlEvent> events,
            List<TlCharacter> characters,
            TlCharacter referenceCharacter,
            String illustrationType,
            String imageStyle) {
        return """
               你是一个架空历史插图提示词生成助手。请根据小说文本和相关档案生成原创插图提示词，不要直接复制游戏截图。
               不要出现游戏 UI、按钮、数值面板、鼠标指针、截图边框。输出 JSON，不要输出 Markdown。
               必须围绕“选定小说片段”设计画面；如果提供了人物参考图，请把它作为主要人物外观参考。
               prompt 字段必须是简洁、可直接生图的英文视觉描述，不要粘贴或复述小说原文。
               如果原文涉及战争、奴役、迫害、仇恨言论或政治冲突，请改写为克制、非血腥、尊重人物的历史纪录画面，
               通过会议、告别、公共空间或象征性景观间接表达，不得复述侮辱性称呼，不得描绘虐待、伤口、尸体、
               仇恨口号、极端主义标志、色情内容或可识别的在世公众人物；画面中的人物均设定为虚构成年人。

               插图类型：%s
               插图风格：%s

               世界线设定：
               %s

               历史阶段：
               %s

               小说正文：
               %s

               选定小说片段：
               %s

               相关事件：
               %s

               相关人物：
               %s

               人物参考图：
               %s

               返回格式：
               {
                 "illustrationTitle": "",
                 "sceneDescription": "",
                 "prompt": "",
                 "negativePrompt": ""
               }
               """
                .formatted(
                        illustrationType,
                        imageStyle,
                        JSON.toJSONString(worldline),
                        JSON.toJSONString(stage),
                        chapterContent,
                        fragmentContent,
                        JSON.toJSONString(events),
                        JSON.toJSONString(characters),
                        formatReferenceCharacter(referenceCharacter));
    }

    private static String formatReferenceCharacter(TlCharacter referenceCharacter) {
        if (referenceCharacter == null) {
            return "Not provided; let the image model create the character appearance.";
        }
        return JSON.toJSONString(referenceCharacter)
                + "\n"
                + "The portrait is available and will be attached as an actual image input during"
                + " final image generation.";
    }

    public static String worldlineChat(
            TlWorldline worldline,
            List<TlEvent> events,
            List<TlStage> stages,
            List<TlNationState> nations,
            List<TlCharacter> characters,
            String question) {
        return """
               你是一个架空历史世界线档案分析助手。请基于系统提供的数据回答用户问题。
               优先依据数据；信息不足时请说明需要补充哪些事件或档案。回答要像历史分析，而不是游戏攻略。

               用户问题：
               %s

               世界线信息：
               %s

               事件列表：
               %s

               阶段列表：
               %s

               国家档案：
               %s

               人物档案：
               %s
               """
                .formatted(
                        question,
                        JSON.toJSONString(worldline),
                        JSON.toJSONString(events),
                        JSON.toJSONString(stages),
                        JSON.toJSONString(nations),
                        JSON.toJSONString(characters));
    }
}
