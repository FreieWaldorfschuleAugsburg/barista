package de.waldorfaugsburg.barista;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import de.waldorfaugsburg.barista.configuration.BaristaConfiguration;
import de.waldorfaugsburg.barista.mdb.MDBInterface;
import de.waldorfaugsburg.barista.payment.PaymentProcessor;
import de.waldorfaugsburg.barista.sound.SoundPlayer;
import de.waldorfaugsburg.mensamax.client.MensaMaxChipReader;
import de.waldorfaugsburg.mensamax.client.MensaMaxClient;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.FileReader;

@Slf4j
public final class BaristaApplication {

    private BaristaConfiguration configuration;

    private MensaMaxChipReader chipReader;
    private MensaMaxClient mensaMaxClient;
    private MDBInterface mdbInterface;
    private SoundPlayer soundPlayer;
    private PaymentProcessor paymentProcessor;

    public void enable() throws Exception {
        try (final JsonReader reader = new JsonReader(new FileReader("config.json"))) {
            configuration = new Gson().fromJson(reader, BaristaConfiguration.class);
        }

        chipReader = new MensaMaxChipReader(configuration.getChipReader().getPath());
        mensaMaxClient = new MensaMaxClient(configuration.getMensaMax().getEndpoint(), configuration.getMensaMax().getApiKey());
        mdbInterface = new MDBInterface(this);
        soundPlayer = new SoundPlayer();
        paymentProcessor = new PaymentProcessor(this);
    }

    public void disable() throws Exception {
        chipReader.close();
        mdbInterface.close();
        paymentProcessor.close();
    }

    public BaristaConfiguration getConfiguration() {
        return configuration;
    }

    public MensaMaxChipReader getChipReader() {
        return chipReader;
    }

    public MensaMaxClient getMensaMaxClient() {
        return mensaMaxClient;
    }

    public MDBInterface getMdbInterface() {
        return mdbInterface;
    }

    public SoundPlayer getSoundPlayer() {
        return soundPlayer;
    }

    public PaymentProcessor getPaymentProcessor() {
        return paymentProcessor;
    }

    public static void main(final String[] args) {
        final BaristaApplication application = new BaristaApplication();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                application.disable();
            } catch (final Exception e) {
                log.error("An error occurred while disabling application", e);
            }
        }));

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        new Thread(() -> {
            try {
                application.enable();
            } catch (final Exception e) {
                log.error("An error occurred while enabling application", e);
                System.exit(1);
            }
        }).start();

        try {
            synchronized (application) {
                application.wait();
            }
        } catch (final InterruptedException e) {
            log.error("An error occurred while interrupting", e);
        }
    }
}
