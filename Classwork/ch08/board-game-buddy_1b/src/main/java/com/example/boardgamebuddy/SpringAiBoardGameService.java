package com.example.boardgamebuddy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.AudioParameters;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.AudioParameters.AudioResponseFormat;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.AudioParameters.Voice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor.FILTER_EXPRESSION;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Service
public class SpringAiBoardGameService implements BoardGameService {

  private final ChatClient chatClient;

  public SpringAiBoardGameService(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Value("classpath:/promptTemplates/systemPromptTemplate.st")
  Resource promptTemplate;

  
  @Override
  public AudioAnswer askQuestion(AudioQuestion question, String conversationId) {
    var gameNameMatch = String.format(
            "gameTitle == '%s'",
            normalizeGameTitle(question.gameTitle()));

    Media questionAudio = Media.builder()
        .data(question.questionAudio())
        .mimeType(MimeTypeUtils.parseMimeType("audio/mp3"))
        .build();                     

    var chatResponse = chatClient.prompt()
        .user(userSpec -> userSpec
            .text("Answer the question from the given audio file.")
            .media(questionAudio))   
        .system(systemSpec -> systemSpec
            .text(promptTemplate)
            .param("gameTitle", question.gameTitle()))
        .advisors(advisorSpec -> advisorSpec
            .param(FILTER_EXPRESSION, gameNameMatch)
            .param(CONVERSATION_ID, conversationId))

        .options(OpenAiChatOptions.builder()
            .outputModalities(List.of("text", "audio"))   
            .outputAudio(
                new AudioParameters(
                    Voice.ALLOY, AudioResponseFormat.MP3))
            .build())

        .call()
        .chatResponse();
    var answerAudio = chatResponse.getResult()
        .getOutput()
        .getMedia()
        .getFirst()
        .getDataAsByteArray();      
    return new AudioAnswer(question.gameTitle(), answerAudio);
  }
  


  private String normalizeGameTitle(String in) {
    return in.toLowerCase().replace(' ', '_');
  }

}
