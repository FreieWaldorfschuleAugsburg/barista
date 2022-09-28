package de.waldorfaugsburg.barista.http;

import express.Express;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class HTTPServer implements AutoCloseable {

    private final Express server = new Express();

    public HTTPServer(final int port) {
        setupRoutes();
        server.listen(port);
        log.info("Listening for incoming http requests on port '{}'", port);
    }

    private void setupRoutes() {
        server.get("/", (req, res) -> {
            res.
        });
    }

    @Override
    public void close() {
        server.stop();
    }

    @AllArgsConstructor
    @Getter
    public static final class StatusResponse {
        private ;
    }
}
