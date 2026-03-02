package com.teggr.notebook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.teggr.notebook.config.NotebookProperties;

@SpringBootApplication
@EnableConfigurationProperties(NotebookProperties.class)
public class NotebookApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotebookApplication.class, args);
    }
}
