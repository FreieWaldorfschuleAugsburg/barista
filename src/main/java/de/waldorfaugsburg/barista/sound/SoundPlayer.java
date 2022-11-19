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
                final String command = "mpg123 -f -" + sound.getVolume() + " sound/" + sound.name() + ".mp3";
                final String[] commandAndArgs = new String[]{"/bin/sh", "-c", command};
                final Process process = Runtime.getRuntime().exec(commandAndArgs);
                process.waitFor();

                final int exitValue = process.exitValue();
                if (exitValue != 0) {
                    log.error("Player exit value was: " + exitValue);
                }
                log.info("Playing sound: " + sound.name());
                process.destroy();
            } catch (final IOException | InterruptedException e) {
                log.error("An error occurred while playing sound '{}'", sound.name(), e);
            }
        });
    }
}
