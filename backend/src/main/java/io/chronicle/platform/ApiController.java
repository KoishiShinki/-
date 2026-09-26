package io.chronicle.platform;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import io.chronicle.auth.CurrentUser;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

public abstract class ApiController {
    protected ApiResponse success() {
        return ApiResponse.success();
    }

    protected ApiResponse success(Object data) {
        return ApiResponse.success(data);
    }

    protected ApiResponse error(String message) {
        return ApiResponse.error(message);
    }

    protected ApiResponse toAjax(int rows) {
        return rows > 0 ? success() : error("没有更新记录");
    }

    protected String getUsername() {
        return CurrentUser.username();
    }

    protected Long getUserId() {
        return CurrentUser.id();
    }

    protected void startPage() {
        var req =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();
        int page = parse(req.getParameter("pageNum"), 1),
                size = Math.min(100, parse(req.getParameter("pageSize"), 20));
        PageHelper.startPage(page, size);
    }

    private int parse(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    protected PageResult getDataTable(List<?> rows) {
        return new PageResult(200, "操作成功", rows, new PageInfo<>(rows).getTotal());
    }
}
