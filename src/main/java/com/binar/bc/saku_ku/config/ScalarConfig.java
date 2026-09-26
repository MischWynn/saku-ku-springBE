package com.binar.bc.saku_ku.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Scalar API Reference: /scalar - alternatif tampilan Swagger UI, baca spec yang sama (/v3/api-docs).
// Halamannya file statis (static/scalar.html), di sini cuma dikasih URL pendek tanpa ".html".
@Configuration
public class ScalarConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/scalar").setViewName("forward:/scalar.html");
    }
}
