package com.nordigy.testrestapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.data.rest.configuration.SpringDataRestConfiguration;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import java.util.Collections;

@EnableSwagger2WebMvc
@Configuration
@Import({ BeanValidatorPluginsConfiguration.class, SpringDataRestConfiguration.class })
public class SpringfoxConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.ant("/api/users/**"))
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "Test REST API",
                "This API should be covered by e2e tests",
                "0.1.0",
                "TEST",
                new Contact("Alexander Gruzdev", "https://github.com/Gralll", "alexander.gruzdev@nordigy.ru"),
                "No licence required", "You are welcome", Collections.emptyList());
    }

}