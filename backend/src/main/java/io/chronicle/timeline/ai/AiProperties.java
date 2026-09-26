package io.chronicle.timeline.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "timeline.ai")
public class AiProperties {
    private boolean enabled = false;
    private Integer timeoutSeconds = 180;
    private Integer imageTimeoutSeconds = 600;
    private Integer textMaxTokens = 4096;
    private Integer visionMaxTokens = 1200;
    private Integer eventDraftMaxTokens = 1200;
    private Integer visionImageMaxSide = 1600;
    private Double visionImageQuality = 0.85D;
    private SiliconFlow siliconflow = new SiliconFlow();
    private OpenAi openai = new OpenAi();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getImageTimeoutSeconds() {
        return imageTimeoutSeconds;
    }

    public void setImageTimeoutSeconds(Integer imageTimeoutSeconds) {
        this.imageTimeoutSeconds = imageTimeoutSeconds;
    }

    public Integer getTextMaxTokens() {
        return textMaxTokens;
    }

    public void setTextMaxTokens(Integer textMaxTokens) {
        this.textMaxTokens = textMaxTokens;
    }

    public Integer getVisionMaxTokens() {
        return visionMaxTokens;
    }

    public void setVisionMaxTokens(Integer visionMaxTokens) {
        this.visionMaxTokens = visionMaxTokens;
    }

    public Integer getEventDraftMaxTokens() {
        return eventDraftMaxTokens;
    }

    public void setEventDraftMaxTokens(Integer eventDraftMaxTokens) {
        this.eventDraftMaxTokens = eventDraftMaxTokens;
    }

    public Integer getVisionImageMaxSide() {
        return visionImageMaxSide;
    }

    public void setVisionImageMaxSide(Integer visionImageMaxSide) {
        this.visionImageMaxSide = visionImageMaxSide;
    }

    public Double getVisionImageQuality() {
        return visionImageQuality;
    }

    public void setVisionImageQuality(Double visionImageQuality) {
        this.visionImageQuality = visionImageQuality;
    }

    public SiliconFlow getSiliconflow() {
        return siliconflow;
    }

    public void setSiliconflow(SiliconFlow siliconflow) {
        this.siliconflow = siliconflow;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public static class SiliconFlow {
        private String baseUrl = "https://api.siliconflow.cn/v1";
        private String apiKey = "";
        private String visionModel = "Qwen/Qwen3-VL-32B-Thinking";
        private String textModel = "deepseek-ai/DeepSeek-V3.2";
        private Boolean visionEnableThinking;
        private Boolean textEnableThinking = false;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getVisionModel() {
            return visionModel;
        }

        public void setVisionModel(String visionModel) {
            this.visionModel = visionModel;
        }

        public String getTextModel() {
            return textModel;
        }

        public void setTextModel(String textModel) {
            this.textModel = textModel;
        }

        public Boolean getVisionEnableThinking() {
            return visionEnableThinking;
        }

        public void setVisionEnableThinking(Boolean visionEnableThinking) {
            this.visionEnableThinking = visionEnableThinking;
        }

        public Boolean getTextEnableThinking() {
            return textEnableThinking;
        }

        public void setTextEnableThinking(Boolean textEnableThinking) {
            this.textEnableThinking = textEnableThinking;
        }
    }

    public static class OpenAi {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String imageModel = "gpt-image-1";
        private String quality = "low";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getImageModel() {
            return imageModel;
        }

        public void setImageModel(String imageModel) {
            this.imageModel = imageModel;
        }

        public String getQuality() {
            return quality;
        }

        public void setQuality(String quality) {
            this.quality = quality;
        }
    }
}
