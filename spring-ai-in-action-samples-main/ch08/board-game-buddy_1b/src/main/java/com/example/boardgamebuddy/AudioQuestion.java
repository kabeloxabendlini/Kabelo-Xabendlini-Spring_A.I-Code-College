package com.example.boardgamebuddy;

import org.springframework.core.io.Resource;

public record AudioQuestion(String gameTitle, Resource questionAudio) {
}
