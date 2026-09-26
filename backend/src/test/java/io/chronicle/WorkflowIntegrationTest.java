package io.chronicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.chronicle.auth.AuthService;

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
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired AuthService auth;
    private static final AtomicInteger sequence = new AtomicInteger();
    private static final String PASSWORD = "Test-only-long-password!";

    private String user() throws Exception {
        String username = "reader_" + sequence.incrementAndGet();
        call(
                post("/app/auth/register"),
                null,
                Map.of(
                        "username",
                        username,
                        "password",
                        PASSWORD,
                        "confirmPassword",
                        PASSWORD,
                        "nickname",
                        "Test reader"),
                200);
        return call(
                        post("/app/auth/login"),
                        null,
                        Map.of("username", username, "password", PASSWORD),
                        200)
                .path("token")
                .asText();
    }

    private JsonNode call(
            MockHttpServletRequestBuilder request, String token, Object body, int status)
            throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null)
            request.contentType("application/json").content(json.writeValueAsBytes(body));
        return json.readTree(
                mvc.perform(request)
                        .andExpect(status().is(status))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    private long world(String token) throws Exception {
        return call(
                        post("/app/worldline"),
                        token,
                        Map.of("worldlineName", "Integration world " + sequence.incrementAndGet()),
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
                                "A new law",
                                "eventYear",
                                1848,
                                "summary",
                                "A reform was enacted",
                                "aiDescription",
                                "A fictional narrative fragment"),
                        200)
                .path("data")
                .path("eventId")
                .asLong();
    }

    private long stage(String token, long world) throws Exception {
        return call(
                        post("/app/stage"),
                        token,
                        Map.of("worldlineId", world, "stageName", "Reform era", "startYear", 1848),
                        200)
                .path("data")
                .path("stageId")
                .asLong();
    }

    @Test
    void accountRegistrationLoginLogoutUsesHashedSessions() throws Exception {
        String token = user();
        JsonNode profile = call(get("/app/auth/profile"), token, null, 200);
        assertThat(profile.path("user").path("roles").get(0).asText()).isEqualTo("USER");
        assertThat(
                        db.queryForObject(
                                "select count(*) from app_session where token_hash=?",
                                Integer.class,
                                token))
                .isZero();
        call(post("/app/auth/logout"), token, null, 200);
        call(get("/app/auth/profile"), token, null, 401);
    }

    @Test
    void unauthorizedAndNonAdminRequestsAreRejected() throws Exception {
        call(get("/app/worldline/myList"), null, null, 401);
        call(get("/timeline/worldline/list"), user(), null, 403);
        call(
                post("/app/auth/register"),
                null,
                Map.of("username", "weakuser", "password", "123", "confirmPassword", "123"),
                400);
        call(
                post("/app/auth/login").header("Origin", "https://untrusted.example"),
                null,
                Map.of("username", "unknown", "password", PASSWORD),
                403);
    }

    @Test
    void worldsStayPrivateUntilPublishedAndCannotBeEditedByOthers() throws Exception {
        String owner = user(), stranger = user();
        long id = world(owner);
        call(get("/public/worldline/" + id), null, null, 400);
        call(get("/app/worldline/" + id), stranger, null, 403);
        call(
                put("/app/worldline"),
                stranger,
                Map.of("worldlineId", id, "worldlineName", "Hijack"),
                403);
        call(
                post("/app/worldline/setVisibility"),
                owner,
                Map.of("worldlineId", id, "visibility", "1"),
                200);
        JsonNode published = call(get("/public/worldline/" + id), null, null, 200);
        assertThat(published.path("data").path("creatorId").asLong()).isPositive();
        long privateWorld = world(owner);
        call(
                put("/app/worldline"),
                owner,
                Map.of("worldlineId", id, "remark", "Private editorial note"),
                200);
        JsonNode creator =
                call(
                                get(
                                        "/public/creator/"
                                                + published
                                                        .path("data")
                                                        .path("creatorId")
                                                        .asLong()),
                                null,
                                null,
                                200)
                        .path("data");
        assertThat(creator.path("worldlines").size()).isEqualTo(1);
        assertThat(creator.path("worldlines").get(0).path("worldlineId").asLong())
                .isEqualTo(id)
                .isNotEqualTo(privateWorld);
        assertThat(creator.path("worldlines").get(0).has("remark")).isFalse();
        assertThat(creator.path("worldlines").get(0).has("createBy")).isFalse();
        call(get("/public/worldline/list").param("pageSize", "2"), null, null, 200);
    }

    @Test
    void eventAndStageWorkflowAndCrossWorldBatchBindingAreAtomic() throws Exception {
        String owner = user(), other = user();
        long ownWorld = world(owner), foreignWorld = world(other);
        long ownEvent = event(owner, ownWorld),
                foreignEvent = event(other, foreignWorld),
                stageId = stage(owner, ownWorld);
        call(
                post("/app/stage/bindEvents"),
                owner,
                Map.of("stageId", stageId, "eventIds", new long[] {ownEvent, foreignEvent}),
                403);
        assertThat(
                        db.queryForObject(
                                "select stage_id from tl_event where event_id=?",
                                Long.class,
                                ownEvent))
                .isNull();
        call(
                post("/app/stage/bindEvents"),
                owner,
                Map.of("stageId", stageId, "eventIds", new long[] {ownEvent}),
                200);
        call(post("/app/event/markDivergence/" + ownEvent), owner, null, 200);
        JsonNode stat = call(get("/app/worldline/stat/" + ownWorld), owner, null, 200);
        assertThat(stat.path("data").path("eventCount").asInt()).isEqualTo(1);
        assertThat(stat.path("data").path("divergenceScore").asInt()).isPositive();
        call(
                post("/app/event/relation"),
                owner,
                Map.of(
                        "worldlineId",
                        ownWorld,
                        "sourceEventId",
                        ownEvent,
                        "targetEventId",
                        foreignEvent),
                403);
        call(
                put("/app/event"),
                owner,
                Map.of("eventId", ownEvent, "stageId", stage(other, foreignWorld)),
                403);
    }

    @Test
    void publishingStripsPrivatePromptAndExportsBothFormats() throws Exception {
        String token = user();
        long world = world(token);
        long chapter =
                call(
                                post("/app/chapter"),
                                token,
                                Map.of(
                                        "worldlineId",
                                        world,
                                        "chapterTitle",
                                        "The reform",
                                        "content",
                                        "A long fictional chapter.",
                                        "aiPrompt",
                                        "private narrative context"),
                                200)
                        .path("data")
                        .path("chapterId")
                        .asLong();
        call(
                post("/app/chapter/setPublic"),
                token,
                Map.of("chapterId", chapter, "publish", true),
                200);
        call(
                post("/app/worldline/setVisibility"),
                token,
                Map.of("worldlineId", world, "visibility", "1"),
                200);
        JsonNode chapters = call(get("/public/chapter/list/" + world), null, null, 200);
        assertThat(chapters.path("data").get(0).path("aiPrompt").isNull()).isTrue();
        assertThat(
                        call(post("/app/chapter/exportMarkdown/" + chapter), token, null, 200)
                                .path("data")
                                .asText())
                .contains("# The reform");
        mvc.perform(
                        post("/app/chapter/exportWord/" + chapter)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .contentType(
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    void disabledAiCreatesExplicitFailureAndTaskIdsArePrivate() throws Exception {
        String owner = user(), stranger = user();
        long world = world(owner);
        call(
                post("/app/chat/ask"),
                owner,
                Map.of("worldlineId", world, "question", "What happened?"),
                503);
        JsonNode tasks = call(get("/app/aiTask/recent"), owner, null, 200).path("data");
        assertThat(tasks.get(0).path("taskStatus").asText()).isEqualTo("3");
        long id = tasks.get(0).path("taskId").asLong();
        call(get("/app/aiTask/status/" + id), stranger, null, 403);
        call(delete("/app/chat/history/" + id), stranger, null, 403);
        call(post("/app/aiTask/retry/" + id), owner, null, 503);
        assertThat(
                        db.queryForObject(
                                "select task_status from tl_ai_task where task_id=?",
                                String.class,
                                id))
                .isEqualTo("3");
    }

    private MockMultipartFile png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        return new MockMultipartFile(
                "file", "../../private-name.png", "image/png", out.toByteArray());
    }

    @Test
    void privateUploadsRequireOwnershipAndCannotBeGraftedIntoPublicProfile() throws Exception {
        String owner = user(), stranger = user();
        long world = world(owner);
        String raw =
                mvc.perform(
                                multipart("/app/screenshot/upload")
                                        .file(png())
                                        .param("worldlineId", String.valueOf(world))
                                        .header("Authorization", "Bearer " + owner))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String url = json.readTree(raw).path("data").path("imageUrl").asText();
        assertThat(url).startsWith("/profile/").doesNotContain("private-name").doesNotContain("..");
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get(url).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
        mvc.perform(get(url).header("Authorization", "Bearer " + owner)).andExpect(status().isOk());
        call(put("/app/auth/profile"), stranger, Map.of("avatarUrl", url), 403);
        long foreignWorld = world(stranger);
        call(
                put("/app/worldline"),
                stranger,
                Map.of("worldlineId", foreignWorld, "coverUrl", url, "visibility", "1"),
                403);
        call(
                post("/app/character"),
                stranger,
                Map.of(
                        "worldlineId",
                        foreignWorld,
                        "characterName",
                        "Reference",
                        "portraitUrl",
                        url),
                403);
        mvc.perform(
                        multipart("/app/screenshot/upload")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "x.png",
                                                "image/png",
                                                "not-an-image".getBytes()))
                                .param("worldlineId", String.valueOf(world))
                                .header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crossWorldChapterAndIllustrationReferencesAreRejected() throws Exception {
        String owner = user(), stranger = user();
        long ownWorld = world(owner), foreignWorld = world(stranger);
        long ownEvent = event(owner, ownWorld), foreignStage = stage(stranger, foreignWorld);
        long foreignChapter =
                call(
                                post("/app/chapter"),
                                stranger,
                                Map.of(
                                        "worldlineId",
                                        foreignWorld,
                                        "chapterTitle",
                                        "Private chapter",
                                        "content",
                                        "Private content"),
                                200)
                        .path("data")
                        .path("chapterId")
                        .asLong();
        call(
                post("/app/illustration/generatePrompt"),
                owner,
                Map.of("worldlineId", ownWorld, "eventId", ownEvent, "chapterId", foreignChapter),
                403);
        call(
                post("/app/chapter"),
                owner,
                Map.of(
                        "worldlineId",
                        ownWorld,
                        "stageId",
                        foreignStage,
                        "chapterTitle",
                        "Bad link"),
                403);
    }

    @Test
    void adminRoleAndKnowledgeCrudRoutesAreAvailable() throws Exception {
        String name = "admin_" + sequence.incrementAndGet();
        auth.register(name, "Test admin", PASSWORD, "ADMIN");
        String token =
                call(
                                post("/app/auth/login"),
                                null,
                                Map.of("username", name, "password", PASSWORD),
                                200)
                        .path("token")
                        .asText();
        call(get("/timeline/worldline/list"), token, null, 200);
        long world = world(token);
        call(
                post("/timeline/nation"),
                token,
                Map.of("worldlineId", world, "countryName", "Fictional country"),
                200);
        call(
                get("/timeline/nation/list").param("worldlineId", String.valueOf(world)),
                token,
                null,
                200);
        call(
                post("/timeline/character"),
                token,
                Map.of("worldlineId", world, "characterName", "Fictional adult"),
                200);
        call(get("/timeline/analysis/" + world), token, null, 200);
    }

    @Test
    void deletingWorldlineRemovesDependentPrivateRecords() throws Exception {
        String token = user();
        long world = world(token);
        event(token, world);
        stage(token, world);
        call(delete("/app/worldline/" + world), token, null, 200);
        assertThat(
                        db.queryForObject(
                                "select count(*) from tl_event where worldline_id=?",
                                Integer.class,
                                world))
                .isZero();
        assertThat(
                        db.queryForObject(
                                "select count(*) from tl_stage where worldline_id=?",
                                Integer.class,
                                world))
                .isZero();
    }
}
