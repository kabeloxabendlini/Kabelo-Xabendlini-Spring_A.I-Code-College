package com.example.boardgamebuddy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SelfEvaluatingBoardGameService implements BoardGameService {

    private final ChatClient chatClient;
    private final RelevancyEvaluator evaluator;

    public SelfEvaluatingBoardGameService(ChatClient.Builder chatClientBuilder) {

        // Use the local Gemma model to generate answers.
        var chatOptions = ChatOptions.builder()
                .model("gemma:2b")
                .build();

        this.chatClient = chatClientBuilder
                .defaultOptions(chatOptions)
                .build();

        // Create a separate client for evaluating answer relevance.
        ChatClient.Builder evaluatorClientBuilder = chatClientBuilder.clone()
                .defaultOptions(chatOptions);

        // Instruct the model to return only YES or NO.
        PromptTemplate evaluationPrompt = new PromptTemplate("""
                You are evaluating whether an answer is relevant to a question.

                Question:
                {query}

                Answer:
                {response}

                Is the answer relevant to the question?

                Reply with exactly one word:
                YES
                or
                NO

                Do not include punctuation or any explanation.
                """);

        this.evaluator = RelevancyEvaluator.builder()
                .chatClientBuilder(evaluatorClientBuilder)
                .promptTemplate(evaluationPrompt)
                .build();
    }

    @Override
    @Retryable(retryFor = AnswerNotRelevantException.class)
    public Answer askQuestion(Question question) {

        // Generate an answer to the user's question.
        var answerText = chatClient.prompt()
                .user(question.question())
                .call()
                .content();

        // Check whether the answer is relevant.
        evaluateRelevancy(question, answerText);

        return new Answer(answerText);
    }

    @Recover
    public Answer recover(AnswerNotRelevantException exception) {

        // Return a fallback if all attempts fail the relevance check.
        return new Answer(
                "I'm sorry, I wasn't able to answer the question."
        );
    }

    private void evaluateRelevancy(Question question, String answerText) {

        var evaluationRequest =
                new EvaluationRequest(question.question(), answerText);

        EvaluationResponse evaluationResponse =
                evaluator.evaluate(evaluationRequest);

        System.out.println("DEBUG - Generated answer: " + answerText);
        System.out.println(
                "DEBUG - Relevancy passed: " + evaluationResponse.isPass()
        );
        System.out.println(
                "DEBUG - Relevancy feedback: " + evaluationResponse.getFeedback()
        );

        if (!evaluationResponse.isPass()) {
            throw new AnswerNotRelevantException(
                    question.question(),
                    answerText
            );
        }
    }
}

