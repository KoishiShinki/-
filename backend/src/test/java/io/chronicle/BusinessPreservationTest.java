package io.chronicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import io.chronicle.timeline.ai.AiProperties;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

/**
 * Exercises actual HTTP serialization against a loopback provider, never an external AI account.
 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:business_preservation;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "chronicle.storage-root=./target/business-test-uploads"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessPreservationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired AiProperties ai;

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String PASSWORD = "Synthetic-test-password-42!";
    private static final String PROVIDER_KEY = "synthetic-loopback-provider-key";
    private final LinkedBlockingQueue<Reply> replies = new LinkedBlockingQueue<>();
    private final List<ProviderRequest> requests = new CopyOnWriteArrayList<>();
    private HttpServer provider;

    private record Reply(int status, String body) {}

    private record ProviderRequest(
            String path, String contentType, String authorization, String body) {}

    @BeforeEach
    void startLocalProvider() throws Exception {
        provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext(
                "/v1/",
                exchange -> {
                    byte[] body = exchange.getRequestBody().readAllBytes();
                    requests.add(
                            new ProviderRequest(
                                    exchange.getRequestURI().getPath(),
                                    exchange.getRequestHeaders().getFirst("Content-Type"),
                                    exchange.getRequestHeaders().getFirst("Authorization"),
                                    new String(body, StandardCharsets.ISO_8859_1)));
                    Reply reply = replies.poll();
                    if (reply == null)
                        reply = new Reply(500, "{\"error\":\"unexpected local provider request\"}");
                    byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(reply.status(), bytes.length);
                    try (var out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                });
        provider.start();
        String base = "http://127.0.0.1:" + provider.getAddress().getPort() + "/v1";
        ai.setEnabled(true);
        ai.setTimeoutSeconds(5);
        ai.setImageTimeoutSeconds(5);
        ai.getSiliconflow().setBaseUrl(base);
        ai.getSiliconflow().setApiKey(PROVIDER_KEY);
        ai.getSiliconflow().setVisionModel("test-vision-model");
        ai.getSiliconflow().setTextModel("test-text-model");
        ai.getOpenai().setBaseUrl(base);
        ai.getOpenai().setApiKey(PROVIDER_KEY);
        ai.getOpenai().setImageModel("test-image-model");
    }

    @AfterEach
    void stopLocalProvider() {
        ai.setEnabled(false);
        ai.getSiliconflow().setApiKey("");
        ai.getOpenai().setApiKey("");
        if (provider != null) provider.stop(0);
    }

    private JsonNode call(
            MockHttpServletRequestBuilder request, String token, Object body, int expected)
            throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null)
            request.contentType("application/json").content(json.writeValueAsBytes(body));
        return json.readTree(
                mvc.perform(request)
                        .andExpect(status().is(expected))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8));
    }

    private String user() throws Exception {
        String username = "business_" + SEQUENCE.incrementAndGet();
        call(
                post("/app/auth/register"),
                null,
                Map.of(
                        "username",
                        username,
                        "nickname",
                        "Synthetic reader",
                        "password",
                        PASSWORD,
                        "confirmPassword",
                        PASSWORD),
                200);
        return call(
                        post("/app/auth/login"),
                        null,
                        Map.of("username", username, "password", PASSWORD),
                        200)
                .path("token")
                .asText();
    }

    private long world(String token) throws Exception {
        return call(
                        post("/app/worldline"),
                        token,
                        Map.of("worldlineName", "Synthetic world " + SEQUENCE.incrementAndGet()),
                        200)
                .path("data")
                .path("worldlineId")
                .asLong();
    }

    private long event(String token, long world) throws Exception {
        return call(
                        post("/app/event"),
                        token,
                        Map.of(
                                "worldlineId",
                                world,
                                "eventTitle",
                                "A fictional reform",
                                "eventYear",
                                1848,
                                "aiDescription",
                                "An adult fictional character visits the parliament."),
                        200)
                .path("data")
                .path("eventId")
                .asLong();
    }

    private byte[] png() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }

    private JsonNode upload(String endpoint, String token, long world) throws Exception {
        var request =
                multipart(endpoint)
                        .file(new MockMultipartFile("file", "synthetic.png", "image/png", png()))
                        .param("worldlineId", String.valueOf(world))
                        .header("Authorization", "Bearer " + token);
        return json.readTree(
                        mvc.perform(request)
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(StandardCharsets.UTF_8))
                .path("data");
    }

    private void textReply(String text) throws Exception {
        replies.add(
                new Reply(
                        200,
                        json.writeValueAsString(
                                Map.of(
                                        "choices",
                                        List.of(Map.of("message", Map.of("content", text)))))));
    }

    private void imageReply() throws Exception {
        replies.add(
                new Reply(
                        200,
                        json.writeValueAsString(
                                Map.of(
                                        "data",
                                        List.of(
                                                Map.of(
                                                        "b64_json",
                                                        Base64.getEncoder()
                                                                .encodeToString(png())))))));
    }

    @Test
    void visionUsesUploadedPixelsAndDraftConfirmationIsNotDuplicated() throws Exception {
        String token = user();
        long world = world(token);
        JsonNode screenshot = upload("/app/screenshot/upload", token, world);
        long id = screenshot.path("screenshotId").asLong();
        String draft =
                json.writeValueAsString(
                        Map.of(
                                "eventTitle",
                                "Parliament opened",
                                "eventYear",
                                1848,
                                "eventType",
                                "politics",
                                "summary",
                                "A synthetic reform",
                                "relatedForces",
                                List.of("Council")));
        textReply("Synthetic visual summary");
        textReply(draft);
        JsonNode recognized = call(post("/app/screenshot/recognize/" + id), token, null, 200);
        assertThat(recognized.path("data").path("draft").path("eventTitle").asText())
                .isEqualTo("Parliament opened");
        assertThat(requests).hasSize(2);
        assertThat(requests)
                .allSatisfy(
                        r -> {
                            assertThat(r.path()).isEqualTo("/v1/chat/completions");
                            assertThat(r.authorization()).isEqualTo("Bearer " + PROVIDER_KEY);
                        });
        JsonNode vision = json.readTree(requests.get(0).body());
        assertThat(vision.path("model").asText()).isEqualTo("test-vision-model");
        String pixels =
                vision.path("messages")
                        .get(0)
                        .path("content")
                        .get(1)
                        .path("image_url")
                        .path("url")
                        .asText();
        assertThat(pixels).startsWith("data:image/jpeg;base64,");
        assertThat(
                        ImageIO.read(
                                        new ByteArrayInputStream(
                                                Base64.getDecoder()
                                                        .decode(pixels.split(",", 2)[1])))
                                .getWidth())
                .isEqualTo(3);
        assertThat(
                        json.readTree(requests.get(1).body())
                                .path("messages")
                                .get(0)
                                .path("content")
                                .asText())
                .contains("Synthetic visual summary");

        call(post("/app/screenshot/rejectDraft"), token, Map.of("screenshotId", id), 200);
        call(post("/app/screenshot/confirmDraft"), token, Map.of("screenshotId", id), 400);
        assertThat(
                        db.queryForObject(
                                "select count(*) from tl_event where worldline_id=?",
                                Integer.class,
                                world))
                .isZero();
        textReply("Synthetic visual summary");
        textReply(draft);
        call(post("/app/screenshot/recognize/" + id), token, null, 200);
        long event =
                call(post("/app/screenshot/confirmDraft"), token, Map.of("screenshotId", id), 200)
                        .path("data")
                        .path("eventId")
                        .asLong();
        call(post("/app/screenshot/confirmDraft"), token, Map.of("screenshotId", id), 400);
        assertThat(
                        db.queryForObject(
                                "select count(*) from tl_event where worldline_id=?",
                                Integer.class,
                                world))
                .isEqualTo(1);
        call(delete("/app/screenshot/" + id), token, null, 200);
        assertThat(
                        db.queryForObject(
                                "select screenshot_id from tl_event where event_id=?",
                                Long.class,
                                event))
                .isNull();
    }

    @Test
    void generatedImagesAndPortraitEditsPreservePublicationBoundaries() throws Exception {
        String token = user();
        long world = world(token), event = event(token, world);
        imageReply();
        JsonNode picture =
                call(
                                post("/app/illustration/generateImage"),
                                token,
                                Map.of(
                                        "worldlineId",
                                        world,
                                        "eventId",
                                        event,
                                        "prompt",
                                        "Synthetic Victorian parliament",
                                        "negativePrompt",
                                        "readable text"),
                                200)
                        .path("data");
        long id = picture.path("illustrationId").asLong();
        String url = picture.path("imageUrl").asText();
        assertThat(url).startsWith("/profile/");
        JsonNode payload = json.readTree(requests.get(0).body());
        assertThat(requests.get(0).path()).isEqualTo("/v1/images/generations");
        assertThat(payload.path("model").asText()).isEqualTo("test-image-model");
        assertThat(payload.path("prompt").asText())
                .contains("Synthetic Victorian parliament", "readable text");
        assertThat(payload.path("n").asInt()).isEqualTo(1);
        mvc.perform(get(url)).andExpect(status().isNotFound());
        byte[] saved =
                mvc.perform(get(url).header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        assertThat(ImageIO.read(new ByteArrayInputStream(saved)).getHeight()).isEqualTo(2);
        call(post("/app/illustration/adopt/" + id), token, null, 200);
        call(
                post("/app/worldline/setVisibility"),
                token,
                Map.of("worldlineId", world, "visibility", "1"),
                200);
        JsonNode published =
                call(get("/public/illustration/list/" + world), null, null, 200)
                        .path("data")
                        .get(0);
        assertThat(published.path("aiRawResult").isNull()).isTrue();
        assertThat(published.path("prompt").isNull()).isTrue();
        assertThat(published.path("negativePrompt").isNull()).isTrue();
        mvc.perform(get(url)).andExpect(status().isOk());
        call(post("/app/illustration/discard/" + id), token, null, 200);
        mvc.perform(get(url)).andExpect(status().isNotFound());

        JsonNode portrait = upload("/app/character/uploadPortrait", token, world);
        imageReply();
        call(
                post("/app/illustration/generateImage"),
                token,
                Map.of(
                        "worldlineId",
                        world,
                        "eventId",
                        event,
                        "characterId",
                        portrait.path("characterId").asLong(),
                        "prompt",
                        "Synthetic adult portrait"),
                200);
        ProviderRequest edit = requests.get(1);
        assertThat(edit.path()).isEqualTo("/v1/images/edits");
        assertThat(edit.contentType()).startsWith("multipart/form-data; boundary=");
        assertThat(edit.authorization()).isEqualTo("Bearer " + PROVIDER_KEY);
        assertThat(edit.body())
                .contains(
                        "name=\"model\"",
                        "test-image-model",
                        "name=\"image\"; filename=\"reference.jpg\"",
                        "Content-Type: image/jpeg",
                        "Synthetic adult portrait");
    }

    @Test
    void providerErrorsDoNotLeakAndRetryActuallyCallsProviderAgain() throws Exception {
        String token = user();
        long world = world(token);
        replies.add(
                new Reply(401, "{\"error\":{\"message\":\"SYNTHETIC_PRIVATE_PROVIDER_MARKER\"}}"));
        JsonNode error =
                call(
                        post("/app/chat/ask"),
                        token,
                        Map.of("worldlineId", world, "question", "Describe reform"),
                        502);
        assertThat(error.toString())
                .doesNotContain("SYNTHETIC_PRIVATE_PROVIDER_MARKER", PROVIDER_KEY);
        JsonNode task = call(get("/app/aiTask/recent"), token, null, 200).path("data").get(0);
        long taskId = task.path("taskId").asLong();
        assertThat(task.path("taskStatus").asText()).isEqualTo("3");
        assertThat(task.path("errorMsg").asText())
                .doesNotContain("SYNTHETIC_PRIVATE_PROVIDER_MARKER", PROVIDER_KEY);
        textReply("A synthetic successful retry.");
        call(post("/app/aiTask/retry/" + taskId), token, null, 200);
        assertThat(requests).hasSize(2);
        assertThat(
                        db.queryForObject(
                                "select task_status from tl_ai_task where task_id=?",
                                String.class,
                                taskId))
                .isEqualTo("2");
        assertThat(
                        db.queryForObject(
                                "select count(*) from tl_ai_task where worldline_id=? and"
                                    + " result_content=?",
                                Integer.class,
                                world,
                                "A synthetic successful retry."))
                .isEqualTo(1);
    }

    @Test
    void wordExportContainsChineseParagraphsAndRequiresChapterOwnership() throws Exception {
        String token = user(), stranger = user();
        long world = world(token);
        String title = "改革纪事", text = "第一段：议会开幕。\n第二段：城市变迁。";
        long chapter =
                call(
                                post("/app/chapter"),
                                token,
                                Map.of(
                                        "worldlineId",
                                        world,
                                        "chapterTitle",
                                        title,
                                        "content",
                                        text,
                                        "aiPrompt",
                                        "Synthetic private prompt"),
                                200)
                        .path("data")
                        .path("chapterId")
                        .asLong();
        mvc.perform(post("/app/chapter/exportWord/" + chapter))
                .andExpect(status().isUnauthorized());
        call(post("/app/chapter/exportWord/" + chapter), stranger, null, 403);
        var response =
                mvc.perform(
                                post("/app/chapter/exportWord/" + chapter)
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();
        assertThat(response.getHeader("Content-Disposition"))
                .contains("filename*=UTF-8''", ".docx");
        try (var doc =
                new XWPFDocument(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertThat(doc.getParagraphs().stream().map(p -> p.getText()).toList())
                    .containsExactly(title, "第一段：议会开幕。", "第二段：城市变迁。");
        }
        String markdown =
                call(post("/app/chapter/exportMarkdown/" + chapter), token, null, 200)
                        .path("data")
                        .asText();
        assertThat(markdown)
                .contains("# " + title, text)
                .doesNotContain("Synthetic private prompt");
        assertThat(requests).isEmpty();
    }

    @Test
    void deletingAWorldCascadesEveryBusinessRecordButKeepsAnotherWorld() throws Exception {
        String token = user();
        long world = world(token), survivor = world(token);
        long first = event(token, world),
                second = event(token, world),
                survivingEvent = event(token, survivor);
        long stage =
                call(
                                post("/app/stage"),
                                token,
                                Map.of("worldlineId", world, "stageName", "Reform era"),
                                200)
                        .path("data")
                        .path("stageId")
                        .asLong();
        call(
                post("/app/event/relation"),
                token,
                Map.of("worldlineId", world, "sourceEventId", first, "targetEventId", second),
                200);
        call(
                post("/app/nation"),
                token,
                Map.of("worldlineId", world, "countryName", "Synthetic country"),
                200);
        call(
                post("/app/character"),
                token,
                Map.of("worldlineId", world, "characterName", "Synthetic adult"),
                200);
        upload("/app/screenshot/upload", token, world);
        long chapter =
                call(
                                post("/app/chapter"),
                                token,
                                Map.of(
                                        "worldlineId",
                                        world,
                                        "stageId",
                                        stage,
                                        "chapterTitle",
                                        "Synthetic chapter",
                                        "content",
                                        "Synthetic text"),
                                200)
                        .path("data")
                        .path("chapterId")
                        .asLong();
        imageReply();
        long picture =
                call(
                                post("/app/illustration/generateImage"),
                                token,
                                Map.of(
                                        "worldlineId",
                                        world,
                                        "eventId",
                                        first,
                                        "chapterId",
                                        chapter,
                                        "stageId",
                                        stage,
                                        "prompt",
                                        "Synthetic archive"),
                                200)
                        .path("data")
                        .path("illustrationId")
                        .asLong();
        call(
                post("/app/illustration/setChapterCover"),
                token,
                Map.of("chapterId", chapter, "illustrationId", picture),
                200);
        for (String table :
                List.of(
                        "tl_event_relation",
                        "tl_ai_task",
                        "tl_illustration",
                        "tl_chapter",
                        "tl_event",
                        "tl_screenshot",
                        "tl_stage",
                        "tl_character",
                        "tl_nation_state")) {
            assertThat(
                            db.queryForObject(
                                    "select count(*) from " + table + " where worldline_id=?",
                                    Integer.class,
                                    world))
                    .isPositive();
        }
        call(delete("/app/worldline/" + world), token, null, 200);
        for (String table :
                List.of(
                        "tl_event_relation",
                        "tl_ai_task",
                        "tl_illustration",
                        "tl_chapter",
                        "tl_event",
                        "tl_screenshot",
                        "tl_stage",
                        "tl_character",
                        "tl_nation_state")) {
            assertThat(
                            db.queryForObject(
                                    "select count(*) from " + table + " where worldline_id=?",
                                    Integer.class,
                                    world))
                    .isZero();
        }
        assertThat(
                        db.queryForObject(
                                "select count(*) from tl_event where event_id=? and worldline_id=?",
                                Integer.class,
                                survivingEvent,
                                survivor))
                .isEqualTo(1);
        call(get("/app/worldline/" + survivor), token, null, 200);
    }
}
