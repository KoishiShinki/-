package io.chronicle.timeline.ai;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public class AiJsonParser {
    private AiJsonParser() {}

    public static JSONObject extractJsonObject(String text) {
        String json = extract(text, '{', '}');
        if (json == null) {
            return new JSONObject();
        }
        try {
            return JSONObject.parseObject(json);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static JSONArray extractJsonArray(String text) {
        String json = extract(text, '[', ']');
        if (json == null) {
            return new JSONArray();
        }
        try {
            return JSONArray.parseArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static String extract(String text, char open, char close) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf(open);
        int end = text.lastIndexOf(close);
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }
}
