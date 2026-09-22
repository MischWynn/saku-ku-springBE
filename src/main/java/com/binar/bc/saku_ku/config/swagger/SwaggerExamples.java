package com.binar.bc.saku_ku.config.swagger;

public final class SwaggerExamples {

    private SwaggerExamples() {}

    public static final String BAD_REQUEST = """
            {"timestamp":"2026-09-21T10:15:30Z","status":400,"error":"Bad Request","message":"email: harus berupa alamat email yang valid, password: minimal 8 karakter"}""";

    public static final String UNAUTHORIZED_LOGIN = """
            {"timestamp":"2026-09-21T10:15:30Z","status":401,"error":"Unauthorized","message":"Email/No HP atau password salah"}""";

    public static final String UNAUTHORIZED_TOKEN = """
            {"timestamp":"2026-09-21T10:15:30Z","status":401,"error":"Unauthorized","message":"Akses ditolak, Anda tidak terautentikasi"}""";

    public static final String FORBIDDEN = """
            {"timestamp":"2026-09-21T10:15:30Z","status":403,"error":"Forbidden","message":"Akses ditolak, Anda tidak memiliki hak akses"}""";

    public static final String NOT_FOUND = """
            {"timestamp":"2026-09-21T10:15:30Z","status":404,"error":"Not Found","message":"Data tidak ditemukan"}""";

    public static final String UNPROCESSABLE_EMAIL_TAKEN = """
            {"timestamp":"2026-09-21T10:15:30Z","status":422,"error":"Unprocessable Entity","message":"Email sudah terdaftar"}""";
}
