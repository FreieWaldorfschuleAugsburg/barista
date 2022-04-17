package de.waldorfaugsburg.barista.sound;

public enum Sound {

    ACCOUNT_OVERDRAWN,
    INVALID_CHIP,
    INVALID_PRODUCT,
    NO_STOCK,
    RESTRICTED,
    SERVICE,
    START,
    TIMEOUT,
    UNKNOWN_ERROR;

    public static Sound findByName(final String soundName) {
        for (final Sound sound : values()) {
            if (sound.name().equalsIgnoreCase(soundName))
                return sound;
        }
        return UNKNOWN_ERROR;
    }
}
