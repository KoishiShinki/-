package io.chronicle.platform;

import java.util.LinkedHashMap;

public class ApiResponse extends LinkedHashMap<String, Object> {
    public ApiResponse(int code, String message) {
        put("code", code);
        put("msg", message);
    }

    public static ApiResponse success() {
        return new ApiResponse(200, "操作成功");
    }

    public static ApiResponse success(Object data) {
        ApiResponse r = success();
        r.put("data", data);
        return r;
    }

    public static ApiResponse error(String msg) {
        return new ApiResponse(400, msg);
    }
}
