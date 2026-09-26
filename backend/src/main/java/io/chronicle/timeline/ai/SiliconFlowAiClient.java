package io.chronicle.timeline.ai;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import io.chronicle.platform.ServiceException;
import io.chronicle.storage.StoragePaths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

@Service
public class SiliconFlowAiClient implements TimelineAiClient {
    private static final Logger log = LoggerFactory.getLogger(SiliconFlowAiClient.class);

    private final AiProperties properties;
    private final io.chronicle.storage.MediaCatalog media;

    public SiliconFlowAiClient(AiProperties properties, io.chronicle.storage.MediaCatalog media) {
        this.properties = properties;
        this.media = media;
    }

    @Override
    public String chat(String prompt) {
        if (!isSiliconFlowEnabled()) {
            throw new ServiceException("AI 文本服务未启用，请先配置服务密钥", 503);
        }
        return postSiliconFlowChat(
                properties.getSiliconflow().getTextModel(), prompt, properties.getTextMaxTokens());
    }

    @Override
    public String vision(String prompt, String imageUrl) {
        if (!isSiliconFlowEnabled()) {
            throw new ServiceException("AI 识图服务未启用，请先配置服务密钥", 503);
        }
        String imagePayload = resolveImagePayload(imageUrl);
        String imageSummary =
                postSiliconFlowVision(properties.getSiliconflow().getVisionModel(), imagePayload);
        String structurePrompt =
                prompt
                        + """

以下是 Qwen 视觉模型基于截图生成的图片描述总结。请你作为 DeepSeek 写作与结构化整理模型，
只依据这份图片总结、世界线设定和原始任务要求，输出可供系统保存的事件草稿 JSON。
游戏身份以原始任务中的最高优先级强制规则为准。如果 Qwen 图片描述把游戏写成《钢铁雄心》、
Hearts of Iron 或 HOI，必须将其视为视觉模型的识别错误，禁止复制、转述或讨论该错误游戏名称，
并继续在《维多利亚3》的语境下保守整理可确认的信息。不确定的信息必须留空或在 remark 中注明。
不要输出 Markdown，不要附加解释。

Qwen 图片描述总结：
"""
                        + imageSummary
                        + """

最终输出前再次检查：只输出事件草稿 JSON，且任何字段都不得出现《钢铁雄心》、
Hearts of Iron、HOI4 或其任何变体。
""";
        return postSiliconFlowChat(
                properties.getSiliconflow().getTextModel(),
                structurePrompt,
                properties.getEventDraftMaxTokens());
    }

    @Override
    public String image(String prompt, String negativePrompt, String referenceImageUrl) {
        if (!isOpenAiImageEnabled()) {
            throw new ServiceException("AI 生图服务未启用，请先配置服务密钥", 503);
        }
        try {
            String finalPrompt = mergeNegativePrompt(prompt, negativePrompt);
            ReferenceImage referenceImage =
                    hasText(referenceImageUrl) ? loadReferenceImage(referenceImageUrl) : null;
            try {
                String response = requestOpenAiImage(finalPrompt, referenceImage);
                return parseImageResponse(response, finalPrompt, false);
            } catch (AiProviderException providerError) {
                if (!providerError.isModerationBlocked()) {
                    logProviderFailure(providerError, false);
                    throw providerServiceException(providerError);
                }
                logProviderFailure(providerError, true);
                String adjustedPrompt = rewriteImagePromptForSafety(finalPrompt);
                try {
                    String response = requestOpenAiImage(adjustedPrompt, referenceImage);
                    return parseImageResponse(response, adjustedPrompt, true);
                } catch (AiProviderException retryError) {
                    logProviderFailure(retryError, retryError.isModerationBlocked());
                    if (retryError.isModerationBlocked()) {
                        throw moderationServiceException(retryError);
                    }
                    throw providerServiceException(retryError);
                }
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI image generation failed", e);
            throw new ServiceException("插图生成失败，请稍后重试；若持续失败，请检查后端图像服务配置。");
        }
    }

    private String requestOpenAiImage(String prompt, ReferenceImage referenceImage)
            throws Exception {
        if (referenceImage != null) {
            return postOpenAiImageEdit(prompt, referenceImage);
        }
        JSONObject body = new JSONObject();
        body.put("model", properties.getOpenai().getImageModel());
        body.put("prompt", prompt);
        body.put("n", 1);
        body.put("size", "1024x1024");
        if (hasText(properties.getOpenai().getQuality())) {
            body.put("quality", properties.getOpenai().getQuality());
        }
        return post(
                properties.getOpenai().getBaseUrl(),
                properties.getOpenai().getApiKey(),
                "/images/generations",
                body.toJSONString(),
                imageTimeout());
    }

    private String postOpenAiImageEdit(String prompt, ReferenceImage referenceImage)
            throws Exception {
        String boundary = "----Chronicle" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeMultipartField(body, boundary, "model", properties.getOpenai().getImageModel());
        writeMultipartField(body, boundary, "prompt", prompt);
        writeMultipartField(body, boundary, "n", "1");
        writeMultipartField(body, boundary, "size", "1024x1024");
        if (hasText(properties.getOpenai().getQuality())) {
            writeMultipartField(body, boundary, "quality", properties.getOpenai().getQuality());
        }
        writeMultipartFile(body, boundary, "image", referenceImage);
        writeUtf8(body, "--" + boundary + "--\r\n");
        return postMultipart(
                properties.getOpenai().getBaseUrl(),
                properties.getOpenai().getApiKey(),
                "/images/edits",
                boundary,
                body.toByteArray(),
                imageTimeout());
    }

    private String parseImageResponse(String response, String promptUsed, boolean safetyAdjusted) {
        JSONObject json = JSONObject.parseObject(response);
        JSONArray data = json.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            throw new ServiceException("插图服务未返回图片数据，请稍后重试。");
        }
        JSONObject first = data.getJSONObject(0);
        JSONObject result = new JSONObject();
        String imageUrl = first.getString("url");
        String b64 = first.getString("b64_json");
        if (!hasText(imageUrl) && hasText(b64)) {
            imageUrl = saveGeneratedImage(b64);
        }
        result.put("imageUrl", imageUrl == null ? "" : imageUrl);
        result.put("promptUsed", promptUsed);
        result.put("safetyAdjusted", safetyAdjusted);
        result.put("raw", "Image generation completed");
        return result.toJSONString();
    }

    private String rewriteImagePromptForSafety(String prompt) {
        if (isSiliconFlowEnabled()) {
            String rewriteInstruction =
                    """
Rewrite the image prompt below into a policy-safe prompt for a fictional historical illustration.
Return only one concise English prompt of at most 120 words. Preserve only the era, setting,
composition, clothing, lighting, palette, and emotional tone. Make every person a fictional adult.
Replace war, enslavement, persecution, humiliation, or political conflict with a calm, indirect,
non-graphic museum-documentary scene. Remove slurs, abusive targeting, blood, wounds, corpses,
sexual content, hateful slogans, extremist symbols, and names or likenesses of living public figures.

Prompt to rewrite:
"""
                            + prompt;
            try {
                String rewritten =
                        postSiliconFlowChat(
                                properties.getSiliconflow().getTextModel(),
                                rewriteInstruction,
                                500);
                rewritten = stripMarkdownFence(rewritten);
                if (hasText(rewritten)) {
                    return rewritten;
                }
            } catch (Exception e) {
                log.warn(
                        "Unable to rewrite a blocked image prompt with the text model; using"
                            + " neutral fallback: {}",
                        e.getMessage());
            }
        }
        return "A neutral museum-style illustration of a fictional nineteenth-century civic"
                   + " interior, fictional adults in period clothing gathered for a formal meeting,"
                   + " calm respectful expressions, soft natural light, restrained historical color"
                   + " palette, non-graphic documentary composition, no readable text, no symbols,"
                   + " no violence, no injuries, no real public figures.";
    }

    private String stripMarkdownFence(String value) {
        if (!hasText(value)) {
            return value;
        }
        String result = value.trim();
        if (result.startsWith("```")) {
            int firstLineEnd = result.indexOf('\n');
            if (firstLineEnd >= 0) {
                result = result.substring(firstLineEnd + 1);
            }
            if (result.endsWith("```")) {
                result = result.substring(0, result.length() - 3);
            }
        }
        return result.trim();
    }

    private void logProviderFailure(AiProviderException error, boolean moderationBlocked) {
        log.warn(
                "OpenAI image request failed: status={}, code={}, requestId={},"
                    + " moderationBlocked={}",
                error.statusCode,
                error.errorCode(),
                error.requestId,
                moderationBlocked);
    }

    private ServiceException moderationServiceException(AiProviderException error) {
        return new ServiceException(
                "插图未通过内容安全检查。系统已自动改写为非血腥、非仇恨的历史画面并重试，"
                        + "但仍未生成。请更换小说片段，或移除侮辱性称呼、血腥伤害、仇恨符号和现实公众人物后再试"
                        + requestIdSuffix(error)
                        + "。");
    }

    private ServiceException providerServiceException(AiProviderException error) {
        return new ServiceException(
                "插图服务请求失败（HTTP "
                        + error.statusCode
                        + "）"
                        + requestIdSuffix(error)
                        + "，请稍后重试或检查后端图像服务配置。");
    }

    private String requestIdSuffix(AiProviderException error) {
        return hasText(error.requestId) ? "，请求编号：" + error.requestId : "";
    }

    private String postSiliconFlowChat(String model, String prompt, Integer maxTokens) {
        return postChat(
                properties.getSiliconflow().getBaseUrl(),
                properties.getSiliconflow().getApiKey(),
                model,
                prompt,
                properties.getSiliconflow().getTextEnableThinking(),
                maxTokens);
    }

    private String postSiliconFlowVision(String model, String imagePayload) {
        JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put(
                "text",
                TimelinePromptBuilder.screenshotGameIdentityRule()
                        + """
                          请识别这张大战略游戏截图，并生成一份中文图片描述总结。
                          重点提取：游戏内时间、国家、事件标题、战争/外交/法律/经济/革命/科技等线索、相关势力、可能影响、画面中可见文字。
                          如果图片信息不足，请明确说明不确定项。不要编写小说，只做截图内容总结。
                          """);

        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", imagePayload);
        JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrl);

        JSONArray content = new JSONArray();
        content.add(textPart);
        content.add(imagePart);

        return postChat(
                properties.getSiliconflow().getBaseUrl(),
                properties.getSiliconflow().getApiKey(),
                model,
                content,
                properties.getSiliconflow().getVisionEnableThinking(),
                properties.getVisionMaxTokens());
    }

    private String postChat(
            String baseUrl,
            String apiKey,
            String model,
            Object content,
            Boolean enableThinking,
            Integer maxTokens) {
        try {
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", content);

            JSONArray messages = new JSONArray();
            messages.add(message);

            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", 0.7);
            if (enableThinking != null) {
                body.put("enable_thinking", enableThinking);
            }
            if (maxTokens != null && maxTokens > 0) {
                body.put("max_tokens", maxTokens);
            }

            String response = post(baseUrl, apiKey, "/chat/completions", body.toJSONString());
            JSONObject json = JSONObject.parseObject(response);
            JSONArray choices = json.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject first = choices.getJSONObject(0);
                JSONObject msg = first.getJSONObject("message");
                if (msg != null && msg.getString("content") != null) {
                    return msg.getString("content");
                }
            }
            return response;
        } catch (Exception e) {
            throw new ServiceException("AI 文本服务请求失败，请检查服务配置后重试", 502);
        }
    }

    private String post(String baseUrl, String apiKey, String path, String body) throws Exception {
        return post(baseUrl, apiKey, path, body, timeout());
    }

    private String post(String baseUrl, String apiKey, String path, String body, int requestTimeout)
            throws Exception {
        String base = trimRight(baseUrl, "/");
        HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeout())).build();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(base + path))
                        .timeout(Duration.ofSeconds(requestTimeout))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiProviderException(
                    response.statusCode(), response.body(), responseRequestId(response));
        }
        return response.body();
    }

    private String postMultipart(
            String baseUrl,
            String apiKey,
            String path,
            String boundary,
            byte[] body,
            int requestTimeout)
            throws Exception {
        String base = trimRight(baseUrl, "/");
        HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeout())).build();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(base + path))
                        .timeout(Duration.ofSeconds(requestTimeout))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiProviderException(
                    response.statusCode(), response.body(), responseRequestId(response));
        }
        return response.body();
    }

    private String responseRequestId(HttpResponse<?> response) {
        return response.headers()
                .firstValue("x-request-id")
                .orElseGet(() -> response.headers().firstValue("request-id").orElse(""));
    }

    private void writeMultipartField(
            ByteArrayOutputStream output, String boundary, String name, String value)
            throws IOException {
        writeUtf8(output, "--" + boundary + "\r\n");
        writeUtf8(output, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        writeUtf8(output, value == null ? "" : value);
        writeUtf8(output, "\r\n");
    }

    private void writeMultipartFile(
            ByteArrayOutputStream output,
            String boundary,
            String name,
            ReferenceImage referenceImage)
            throws IOException {
        writeUtf8(output, "--" + boundary + "\r\n");
        writeUtf8(
                output,
                "Content-Disposition: form-data; name=\""
                        + name
                        + "\"; filename=\""
                        + referenceImage.fileName
                        + "\"\r\n");
        writeUtf8(output, "Content-Type: " + referenceImage.contentType + "\r\n\r\n");
        output.write(referenceImage.bytes);
        writeUtf8(output, "\r\n");
    }

    private void writeUtf8(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isSiliconFlowEnabled() {
        return properties.isEnabled() && hasText(properties.getSiliconflow().getApiKey());
    }

    private boolean isOpenAiImageEnabled() {
        return properties.isEnabled() && hasText(properties.getOpenai().getApiKey());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int timeout() {
        return properties.getTimeoutSeconds() == null ? 60 : properties.getTimeoutSeconds();
    }

    private int imageTimeout() {
        return properties.getImageTimeoutSeconds() == null
                ? 600
                : properties.getImageTimeoutSeconds();
    }

    private String trimRight(String value, String token) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        while (value.endsWith(token)) {
            value = value.substring(0, value.length() - token.length());
        }
        return value;
    }

    private String resolveImagePayload(String imageUrl) {
        media.validate(imageUrl);
        Path imagePath = StoragePaths.resolve(imageUrl);
        if (!Files.isRegularFile(imagePath) || Files.isSymbolicLink(imagePath))
            throw new ServiceException("截图文件不存在");
        try {
            return compressImagePayload(imagePath);
        } catch (Exception e) {
            throw new ServiceException("读取截图失败，请重新上传图片");
        }
    }

    private ReferenceImage loadReferenceImage(String imageUrl) throws IOException {
        if (!imageUrl.startsWith("/profile/")) {
            throw new ServiceException("人物参考图必须是通过本系统上传的图片");
        }
        media.validate(imageUrl);
        Path profileRoot = Path.of(StoragePaths.getProfile()).toAbsolutePath().normalize();
        String relative = imageUrl.substring("/profile/".length()).replace('\\', '/');
        Path imagePath = profileRoot.resolve(relative).normalize();
        if (!imagePath.startsWith(profileRoot) || !Files.isRegularFile(imagePath)) {
            throw new ServiceException("人物参考图文件不存在，请重新上传");
        }
        long fileSize = Files.size(imagePath);
        if (fileSize <= 0 || fileSize > 50L * 1024L * 1024L) {
            throw new ServiceException("人物参考图大小必须在 50MB 以内");
        }

        return compressReferenceImage(imagePath);
    }

    private ReferenceImage compressReferenceImage(Path imagePath) throws IOException {
        BufferedImage source = ImageIO.read(imagePath.toFile());
        if (source == null) {
            throw new ServiceException("人物参考图格式无法识别，请上传 PNG 或 JPG 图片");
        }
        int maxSide = 1536;
        double scale =
                Math.min(1D, (double) maxSide / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(target, "jpg", output);
        return new ReferenceImage(output.toByteArray(), "reference.jpg", "image/jpeg");
    }

    private ReferenceImage convertReferenceImageToPng(Path imagePath) throws IOException {
        BufferedImage source = ImageIO.read(imagePath.toFile());
        if (source == null) {
            throw new ServiceException("人物参考图格式无法识别，请上传 PNG 或 JPG 图片");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(source, "png", output)) {
            throw new ServiceException("人物参考图转换失败，请上传 PNG 或 JPG 图片");
        }
        return new ReferenceImage(output.toByteArray(), "reference.png", "image/png");
    }

    private String compressImagePayload(Path imagePath) {
        try {
            BufferedImage source = ImageIO.read(imagePath.toFile());
            if (source == null) {
                return null;
            }
            int maxSide =
                    properties.getVisionImageMaxSide() == null
                            ? 1600
                            : properties.getVisionImageMaxSide();
            maxSide = Math.max(320, maxSide);
            double scale =
                    Math.min(
                            1D, (double) maxSide / Math.max(source.getWidth(), source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

            BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = target.createGraphics();
            try {
                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(
                        RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
                graphics.drawImage(source, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
                    writer.setOutput(imageOutput);
                    ImageWriteParam params = writer.getDefaultWriteParam();
                    if (params.canWriteCompressed()) {
                        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        params.setCompressionQuality(visionImageQuality());
                    }
                    writer.write(null, new IIOImage(target, null, null), params);
                } finally {
                    writer.dispose();
                }
            } else {
                ImageIO.write(target, "jpg", output);
            }
            return "data:image/jpeg;base64,"
                    + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    private float visionImageQuality() {
        double quality =
                properties.getVisionImageQuality() == null
                        ? 0.85D
                        : properties.getVisionImageQuality();
        return (float) Math.max(0.35D, Math.min(0.95D, quality));
    }

    private String saveGeneratedImage(String b64) {
        try {
            String payload = b64;
            int commaIndex = payload.indexOf(',');
            if (payload.startsWith("data:") && commaIndex >= 0) {
                payload = payload.substring(commaIndex + 1);
            }
            byte[] bytes = Base64.getDecoder().decode(payload);
            Path dir = Path.of(StoragePaths.getProfile(), "timeline", "openai-image");
            Files.createDirectories(dir);
            String fileName = "image_" + UUID.randomUUID() + ".png";
            Files.write(dir.resolve(fileName), bytes);
            String url = "/profile/timeline/openai-image/" + fileName;
            media.register(url);
            return url;
        } catch (Exception e) {
            throw new ServiceException("保存生成图片失败");
        }
    }

    private String mergeNegativePrompt(String prompt, String negativePrompt) {
        if (!hasText(negativePrompt)) {
            return prompt;
        }
        return prompt + "\n\nAvoid these visual elements: " + negativePrompt;
    }

    private static class ReferenceImage {
        private final byte[] bytes;
        private final String fileName;
        private final String contentType;

        private ReferenceImage(byte[] bytes, String fileName, String contentType) {
            this.bytes = bytes;
            this.fileName = fileName;
            this.contentType = contentType;
        }
    }

    private static class AiProviderException extends Exception {
        private final int statusCode;
        private final String responseBody;
        private final String requestId;

        private AiProviderException(int statusCode, String responseBody, String requestId) {
            super("AI provider returned HTTP " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody == null ? "" : responseBody;
            this.requestId = requestId == null ? "" : requestId;
        }

        private boolean isModerationBlocked() {
            String normalized = responseBody.toLowerCase();
            return normalized.contains("moderation_blocked")
                    || normalized.contains("content_policy_violation")
                    || normalized.contains("skipped_mainline");
        }

        private String errorCode() {
            try {
                JSONObject body = JSONObject.parseObject(responseBody);
                JSONObject error = body.getJSONObject("error");
                String code = error == null ? null : error.getString("code");
                return code == null ? "" : code;
            } catch (Exception ignored) {
                return "";
            }
        }
    }
}
