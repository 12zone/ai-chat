package org.example.qasystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration;

@SpringBootApplication(exclude = {DeepSeekChatAutoConfiguration.class})
public class QAsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(QAsystemApplication.class, args);
    }

}
