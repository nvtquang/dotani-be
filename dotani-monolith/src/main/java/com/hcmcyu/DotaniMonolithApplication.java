package com.hcmcyu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication
@ComponentScan(
        basePackages = "com.hcmcyu",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.hcmcyu\\..*\\..*ServiceApplication"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.hcmcyu\\..*\\.security\\.(SecurityConfig|HeaderAuthenticationFilter|JwtAuthenticationFilter|RestAuthenticationEntryPoint)"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.hcmcyu\\..*\\.config\\.(AppConfig|OpenApiConfig)"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.hcmcyu\\..*\\.controller\\.HealthController"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.hcmcyu\\..*\\.exception\\.GlobalExceptionHandler")
        }
)
public class DotaniMonolithApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DotaniMonolithApplication.class);
        application.setBeanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());
        application.run(args);
    }
}
