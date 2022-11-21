package de.waldorfaugsburg.barista.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.waldorfaugsburg.barista.BaristaApplication;
import de.waldorfaugsburg.barista.sound.Sound;
import express.Express;
import express.http.request.Request;
import express.http.response.Response;
import express.utils.Status;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Slf4j
public final class HTTPServer implements AutoCloseable {

    private final BaristaApplication application;

    private final Gson gson = new Gson();
    private final Express server = new Express();

    public HTTPServer(final BaristaApplication application, final int port) {
        this.application = application;

        setupRoutes();
        server.listen(port);
        log.info("Listening for incoming http requests on port '{}'", port);
    }

    private void setupRoutes() {
        server.get("/", (req, res) -> {
            final JsonObject object = new JsonObject();
            object.addProperty("free", application.getPaymentProcessor().isFreeMode());
            res.send(gson.toJson(object));
        });
        server.post("/", (req, res) -> {
            if (isContentTypeInvalid(req, res)) return;

            final Reader reader = new InputStreamReader(req.getBody(), StandardCharsets.UTF_8);
            final JsonObject element = gson.fromJson(reader, JsonObject.class);

            if (!element.has("free")) {
                res.sendStatus(Status._500);
                log.error("Invalid http request: No free state given");
                return;
            }

            final boolean freeMode = element.get("free").getAsBoolean();
            application.getPaymentProcessor().setFreeMode(freeMode);

            res.sendStatus(Status._200);
        });
        server.post("/play", (req, res) -> {
            if (isContentTypeInvalid(req, res)) return;

            final Reader reader = new InputStreamReader(req.getBody(), StandardCharsets.UTF_8);
            final JsonObject element = gson.fromJson(reader, JsonObject.class);

            if (!element.has("sound")) {
                res.sendStatus(Status._500);
                log.error("Invalid http request: No sound given");
                return;
            }

            final String soundName = element.get("sound").getAsString();
            final Sound sound = Sound.findByName(soundName);
            if (sound == null) {
                res.sendStatus(Status._500);
                log.error("Invalid http request: Invalid sound '{}' given", soundName);
                return;
            }

            log.info("Playing sound: {}", sound.name());
            application.getSoundPlayer().play(sound);
            res.sendStatus(Status._200);
        });
    }

    private boolean isContentTypeInvalid(Request req, Response res) {
        if (!req.getContentType().equals("application/json")) {
            res.sendStatus(Status._400);
            log.error("Invalid http request: content-type mismatch");
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        server.stop();
    }
}
