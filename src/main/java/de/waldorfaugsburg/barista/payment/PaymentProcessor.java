package de.waldorfaugsburg.barista.payment;

import de.waldorfaugsburg.barista.BaristaApplication;
import de.waldorfaugsburg.barista.mdb.MDBProduct;
import de.waldorfaugsburg.barista.sound.Sound;
import de.waldorfaugsburg.mensamax.client.api.ApiException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PaymentProcessor implements AutoCloseable {

    private final BaristaApplication application;
    private final Thread listenerThread;

    public PaymentProcessor(final BaristaApplication application) {
        this.application = application;

        listenerThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    final String chipId = application.getChipReader().awaitChip();
                    startPayment(chipId);
                } catch (final InterruptedException ignored) {
                } catch (final Exception e) {
                    log.error("An error error while reading chip", e);
                }
            }
        });
        listenerThread.start();
    }

    @Override
    public void close() {
        listenerThread.interrupt();
    }

    private void startPayment(final String chipId) throws Exception {
        Sound sound = application.getConfiguration().getSounds().get(chipId);
        if (sound == null) {
            sound = Sound.START;
        }
        application.getSoundPlayer().play(sound);

        final MDBProduct product = application.getMdbInterface().awaitProduct();
        if (product == null) {
            application.getMdbInterface().stopSelection();
            application.getSoundPlayer().play(Sound.TIMEOUT);
            log.info("Payment for '{}' timed out", chipId);
            return;
        }

        // Get corresponding product barcode
        final Long productBarcode = application.getConfiguration().getProducts().get(product.productId());
        if (productBarcode == null) {
            application.getMdbInterface().cancelPayment();
            application.getSoundPlayer().play(Sound.INVALID_PRODUCT);
            log.error("Payment for '{}' with invalid product id '{}' requested", chipId, product.productId());
            return;
        }

        // Check if is service chip
        if (!application.getConfiguration().getServiceChipId().equals(chipId)) {
            try {
                application.getMensaMaxClient().transaction(chipId, application.getConfiguration().getMensaMax().getKiosk(), productBarcode);
            } catch (final ApiException e) {
                application.getMdbInterface().cancelPayment();
                application.getSoundPlayer().play(e.getError() == null ? Sound.UNKNOWN_ERROR : Sound.findByName(e.getError().getCode()));
                log.error("Transaction by '{}' for product '{}' ({}€) failed", chipId, product.productId(), product.money(), e);
                return;
            }
        } else {
            application.getSoundPlayer().play(Sound.SERVICE);
        }

        application.getMdbInterface().confirmPayment(product);
        log.info("Successful transaction by '{}' for product '{}' ({}€)", chipId, product.productId(), product.money());
    }
}