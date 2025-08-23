package de.waldorfaugsburg.barista.sound;

import lombok.Getter;

@Getter
public enum Sound {

    SERVICE(20_000),
    START(20_000),
    LASER(20_000),
    WHISTLE(20_000),
    TIMEOUT(20_000),
    INVALID_PRODUCT(100_000),
    API_ERROR(100_000);

    private final int volume;

    Sound(final int volume) {
        this.volume = volume;
    }

    public static Sound findByName(final String soundName) {
        for (final Sound sound : values()) {
            if (sound.name().equalsIgnoreCase(soundName))
                return sound;
        }
        return null;
    }
}
