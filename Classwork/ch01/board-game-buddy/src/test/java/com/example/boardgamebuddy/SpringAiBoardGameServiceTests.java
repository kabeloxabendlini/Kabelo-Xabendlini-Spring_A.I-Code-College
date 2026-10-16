package com.example.boardgamebuddy;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(properties = {
    "spring.ai.openai.api-key=test-key"
})
@EnableWireMock(
    @ConfigureWireMock(
        baseUrlProperties = "spring.ai.openai.base-url"
    )
)
public class SpringAiBoardGameServiceTests {

    @Autowired
    private BoardGameService boardGameService;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @InjectWireMock
    private WireMockServer wireMockServer;

    private RelevancyEvaluator relevancyEvaluator;

    private FactCheckingEvaluator factCheckingEvaluator;

    @BeforeEach
    public void setup() {
        wireMockServer.resetAll();

        this.relevancyEvaluator =
            new RelevancyEvaluator(chatClientBuilder);

        this.factCheckingEvaluator =
            new FactCheckingEvaluator(chatClientBuilder);

        String answerResponse = """
            {
              "id": "chatcmpl-test-answer",
              "object": "chat.completion",
              "created": 1700000000,
              "model": "gpt-3.5-turbo",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "The sky appears blue because air molecules scatter shorter blue wavelengths of sunlight more strongly than longer red wavelengths."
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 10,
                "completion_tokens": 25,
                "total_tokens": 35
              }
            }
            """;

        String relevancyResponse = """
            {
              "id": "chatcmpl-test-evaluation",
              "object": "chat.completion",
              "created": 1700000001,
              "model": "gpt-3.5-turbo",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "YES"
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 10,
                "completion_tokens": 1,
                "total_tokens": 11
              }
            }
            """;

        wireMockServer.stubFor(
            post(urlPathEqualTo("/v1/chat/completions"))
                .inScenario("OpenAI chat requests")
                .whenScenarioStateIs("Started")
                .willReturn(okJson(answerResponse))
                .willSetStateTo("evaluation")
        );

        wireMockServer.stubFor(
            post(urlPathEqualTo("/v1/chat/completions"))
                .inScenario("OpenAI chat requests")
                .whenScenarioStateIs("evaluation")
                .willReturn(okJson(relevancyResponse))
        );
    }

    @Test
    public void evaluateRelevancy() {
        String userText = "Why is the sky blue?";

        Question question = new Question(userText);

        Answer answer = boardGameService.askQuestion(question);

        EvaluationRequest evaluationRequest =
            new EvaluationRequest(userText, answer.answer());

        EvaluationResponse response =
            relevancyEvaluator.evaluate(evaluationRequest);

        org.assertj.core.api.Assertions.assertThat(response.isPass())
            .withFailMessage(
                "The answer was not considered relevant to the question."
            )
            .isTrue();

        wireMockServer.verify(
            2,
            postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
        );
    }

    @Test
    public void evaluateFactualAccuracy() {
        var userText = "Why is the sky blue?";
        var question = new Question(userText);
        var answer = boardGameService.askQuestion(question);

        var evaluationRequest =
            new EvaluationRequest(userText, answer.answer());

        var response =
            factCheckingEvaluator.evaluate(evaluationRequest);

        org.assertj.core.api.Assertions.assertThat(response.isPass())
            .isTrue();
    }
}