package com.example.boardgamebuddy;

import com.example.boardgamebuddy.gamedata.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GameTools {

  private final GameRepository gameRepository;

  public GameTools(GameRepository gameRepository) {  
    this.gameRepository = gameRepository;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(GameTools.class);

  @Tool(name = "listGames",
        description = "Lists the titles of all games that Board Game Buddy has data for.")
  public List<String> listGames() {
      LOGGER.info("Listing all games");
      var titles = new ArrayList<String>();
      gameRepository.findAll().forEach(g -> titles.add(g.title()));
    return titles;
  }
  public GameComplexityResponse getGameComplexity(
            @ToolParam(description="The title of the game") 
            String gameTitle) {
    var gameSlug = gameTitle    
        .toLowerCase()
        .replace(" ", "_");

    LOGGER.info("Getting complexity for {} ({})",
        gameTitle, gameSlug);

    var gameOpt = gameRepository.findBySlug(gameSlug);  

    var game = gameOpt.orElseGet(() -> {  
      LOGGER.warn("Game not found: {}", gameSlug);
      return new Game(
          null,
          gameSlug,
          gameTitle,
          GameComplexity.UNKNOWN.getValue());
    });

    return new GameComplexityResponse(  
        game.title(), game.complexityEnum());
  }

}
