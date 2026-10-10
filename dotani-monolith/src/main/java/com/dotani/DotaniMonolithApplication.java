package com.dotani;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

// Modules share simple class names (DevDataSeeder, LocalStorageService, ...), so beans are named by
// fully qualified class name.
@SpringBootApplication
@ComponentScan(basePackages = "com.dotani", nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class DotaniMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(DotaniMonolithApplication.class, args);
    }
}
