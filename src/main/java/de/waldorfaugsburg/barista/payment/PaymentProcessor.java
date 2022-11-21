package de.waldorfaugsburg.barista.payment;

import de.waldorfaugsburg.barista.BaristaApplication;
import de.waldorfaugsburg.barista.mdb.MDBProduct;
import de.waldorfaugsburg.barista.sound.Sound;
import de.waldorfaugsburg.mensamax.client.api.ApiException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PaymentProcessor implements AutoCloseable {

    private static final int WAIT_MILLIS = 3000;

    private final BaristaApplication application;

    private Thread paymentThread;
    private boolean freeMode;
    private long lastProductAwait;

    public PaymentProcessor(final BaristaApplication application) {
        this.application = application;

        startPaymentThread();
    }

    private void startPaymentThread() {
        if (paymentThread != null) {
            log.info("Interrupting payment thread...");
            paymentThread.interrupt();
        }

        paymentThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    if (freeMode) {
                        // It's necessary to wait a few seconds before awaiting the next product
                        if (System.currentTimeMillis() - lastProductAwait > WAIT_MILLIS) {
                            final MDBProduct product = application.getMdbInterface().awaitProduct(false);
                            if (product != null) {
                                application.getMdbInterface().confirmPayment(product);
                                log.info("Successful request for product '{}' ({}€)", product.productId(), product.money());
                            }

                            lastProductAwait = System.currentTimeMillis();
                        }
                    } else {
                        log.info("Awaiting chip...");
                        final String chip = application.getChipReader().awaitChip();
                        startPayment(chip);
                    }
                } catch (final InterruptedException ignored) {
                    break;
                } catch (final Exception e) {
                    log.error("An error error while handling payment", e);
                }
            }
            log.info("Payment thread was interrupted");
        });
        paymentThread.start();
    }

    @Override
    public void close() {
        paymentThread.interrupt();
    }

    private void startPayment(final String chipId) throws Exception {
        Sound sound = application.getConfiguration().getSounds().get(chipId);
        if (sound == null) {
            sound = Sound.START;
        }
        application.getSoundPlayer().play(sound);

        final MDBProduct product = application.getMdbInterface().awaitProduct(true);
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

                Sound errorSound;
                if (e.getError() == null || (errorSound = Sound.findByName(e.getError().getCode())) == null) {
                    errorSound = Sound.UNKNOWN_ERROR;
                }
                application.getSoundPlayer().play(errorSound);
                log.error("Transaction by '{}' for product '{}' ({}€) failed", chipId, product.productId(), product.money(), e);
                return;
            }
        } else {
            application.getSoundPlayer().play(Sound.SERVICE);
        }

        application.getMdbInterface().confirmPayment(product);
        log.info("Successful transaction by '{}' for product '{}' ({}€)", chipId, product.productId(), product.money());
    }

    public boolean isFreeMode() {
        return freeMode;
    }

    public void setFreeMode(final boolean freeMode) {
        final boolean previouslyEnabled = this.freeMode;
        this.freeMode = freeMode;

        if (previouslyEnabled != freeMode) {
            application.getMdbInterface().stopSelection();
            startPaymentThread();

            log.info(freeMode ? "Free-mode enabled" : "Free-mode disabled");
            application.getSoundPlayer().play(Sound.SERVICE);
        }
    }
}