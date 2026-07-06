package com.example.graduationevaluationsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI 配置（中文）
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("本科生毕业设计评价系统")
                        .description("本科生毕业设计评价系统 API 接口文档")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("毕业设计评价系统")));
    }
}
