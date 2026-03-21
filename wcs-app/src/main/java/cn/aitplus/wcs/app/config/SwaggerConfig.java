package cn.aitplus.wcs.app.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.oas.annotations.EnableOpenApi;

/**
 * Swagger 文档配置。
 */
@Configuration
@EnableOpenApi
@ConditionalOnProperty(prefix = "swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("cn.aitplus.wcs.app.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("WCS API 文档")
                .description("WCS 服务接口文档")
                .version("1.0.0")
                .build();
    }
}
