package de.waldorfaugsburg.barista.sound;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public final class SoundPlayer {

    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public void play(final Sound sound) {
        executorService.submit(() -> {
            try {
                final String command = "mpg123 tts/" + sound.name() + ".mp3";
                final String[] commandAndArgs = new String[]{"/bin/sh", "-c", command};
                final Process process = Runtime.getRuntime().exec(commandAndArgs);
                process.waitFor();

                final int exitValue = process.exitValue();
                if (exitValue != 0) {
                    log.error("TTS exit value was: " + exitValue);
                }
            } catch (final IOException | InterruptedException e) {
                log.error("An error occurred while running process", e);
            }
        });
    }
}
