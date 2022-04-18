package de.waldorfaugsburg.barista.sound;

public enum Sound {

    ACCOUNT_OVERDRAWN(100_000),
    INVALID_CHIP(100_000),
    INVALID_PRODUCT(100_000),
    NO_STOCK(100_000),
    RESTRICTED(100_000),
    SERVICE(20_000),
    START(20_000),
    TIMEOUT(20_000),
    UNKNOWN_ERROR(100_000);

    private final int volume;

    Sound(final int volume) {
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }

    public static Sound findByName(final String soundName) {
        for (final Sound sound : values()) {
            if (sound.name().equalsIgnoreCase(soundName))
                return sound;
        }
        return UNKNOWN_ERROR;
    }
}
