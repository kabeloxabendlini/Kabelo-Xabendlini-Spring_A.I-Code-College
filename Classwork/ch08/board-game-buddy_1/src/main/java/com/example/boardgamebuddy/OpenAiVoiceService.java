package com.example.boardgamebuddy;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Temporary local implementation of VoiceService.
 *
 * This implementation removes the OpenAI dependency so that
 * the application can use Ollama for chat without requiring
 * an OpenAI API key.
 *
 * Voice functionality will be added separately using a local
 * speech-to-text and text-to-speech solution.
 */
@Service
public class OpenAiVoiceService implements VoiceService {

    @Override
    public String transcribe(Resource audioFileResource) {
        throw new UnsupportedOperationException(
            "Local voice transcription has not been configured yet."
        );
    }

    @Override
    public Resource textToSpeech(String text) {
        throw new UnsupportedOperationException(
            "Local text-to-speech has not been configured yet."
        );
    }
}
