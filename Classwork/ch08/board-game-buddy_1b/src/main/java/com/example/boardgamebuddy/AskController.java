package com.example.boardgamebuddy;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AskController {

  private final BoardGameService boardGameService;

  public AskController(BoardGameService boardGameService) {
    this.boardGameService = boardGameService;
  }

  
  @PostMapping(path="/audioAsk", produces = "audio/mpeg")
  public byte[] audioAskAudioResponse(
      @RequestHeader(name="X_AI_CONVERSATION_ID",
          defaultValue = "default") String conversationId,
      @RequestParam("audio") MultipartFile blob,
      @RequestParam("gameTitle") String game) {

    var audioResource = blob.getResource();
    var questionWithAudio = new AudioQuestion(game, audioResource);
    var answer = boardGameService.askQuestion(
        questionWithAudio, conversationId);
    return answer.answerAudio();
  }
  

}
