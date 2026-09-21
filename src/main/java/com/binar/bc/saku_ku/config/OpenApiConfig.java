package com.binar.bc.saku_ku.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Swagger UI: /swagger-ui.html - klik "Authorize" (kunci di kanan atas) terus tempel JWT-nya
// (dari /api/v1/customer/login atau /api/v1/user/login), bukan header manual - security scheme
// di bawah ini yang bikin tombol itu muncul & otomatis nempelin "Bearer <token>" ke tiap request.
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Saku-Ku API",
                version = "v1",
                description = "REST API untuk platform manajemen pinjaman Saku-Ku (Binar Academy capstone). "
                        + "Dua jalur auth terpisah - staff (/api/v1/user/login) dan customer (/api/v1/customer/login) "
                        + "- masing-masing pakai JWT bearer token yang sama formatnya."
        ),
        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
