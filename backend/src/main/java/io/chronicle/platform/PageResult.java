package io.chronicle.platform;

import java.util.List;

public record PageResult(int code, String msg, List<?> rows, long total) {}
