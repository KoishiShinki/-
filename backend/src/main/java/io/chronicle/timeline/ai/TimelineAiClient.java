package io.chronicle.timeline.ai;

public interface TimelineAiClient {
    String chat(String prompt);

    String vision(String prompt, String imageUrl);

    String image(String prompt, String negativePrompt, String referenceImageUrl);
}
