package com.example.boardgamebuddy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

@Service
@Primary
public class SelfEvaluatingBoardGameService implements BoardGameService {

    private final ChatClient chatClient;

    public SelfEvaluatingBoardGameService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Answer askQuestion(Question question) {
        var answerText = chatClient.prompt()
                .user(question.question())
                .call()
                .content();

        return new Answer(answerText);
    }
}

