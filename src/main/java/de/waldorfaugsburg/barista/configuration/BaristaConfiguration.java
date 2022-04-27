package de.waldorfaugsburg.barista.configuration;

import de.waldorfaugsburg.barista.sound.Sound;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@Getter
public final class BaristaConfiguration {

    private String serviceChipId;
    private PivotConfiguration pivot;
    private ChipReaderConfiguration chipReader;
    private MDBConfiguration mdb;
    private SentryConfiguration sentry;
    private Map<Integer, Long> products;
    private Map<String, Sound> sounds;

    @NoArgsConstructor
    @Getter
    public static final class PivotConfiguration {
        private String endpoint;
        private String apiKey;
        private String kiosk;
    }

    @NoArgsConstructor
    @Getter
    public static final class ChipReaderConfiguration {
        private String path;
    }

    @NoArgsConstructor
    @Getter
    public static final class MDBConfiguration {
        private int startMoney;
        private long timeoutMillis;
    }

    @NoArgsConstructor
    @Getter
    public static final class SentryConfiguration {
        private String dsn;
    }
}
