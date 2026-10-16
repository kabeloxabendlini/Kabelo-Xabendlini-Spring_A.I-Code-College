package com.example.summarization;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@SpringBootApplication
public class SummarizationApplication {

  public static void main(String[] args) {
    SpringApplication.run(SummarizationApplication.class, args);
  }

  private static final String SYSTEM_PROMPT = """
      You are a helpful assistant with skills in summarizing the rules for board games.
      Given the rules for a game below, summarize them into a brief set of quick-start instructions.
      
      GAME RULES
      ----------
      """;

  @Value("classpath:/systemPrompt.txt")
  Resource systemPrompt;

  @Value("file:/Users/habuma/Documents/BoardGameRules/ShiftingStones.pdf")
  Resource rulesResource;

  @Bean
  ApplicationRunner go(ChatClient.Builder clientBuilder) {
    return args -> {
      var chatClient = clientBuilder.build();

      var rulesText = new TikaDocumentReader(rulesResource)
          .get().get(0).getText();

      var summary = chatClient.prompt()
//          .system(SYSTEM_PROMPT + "\n" + rulesText)
          .system(systemSpec -> systemSpec.text(rulesText)
              .param("gameRules", rulesText))
          .user("Summarize the rules")
          .call()
          .content();

      System.out.println("Summary: \n--------\n" + summary);
    };
  }

}
