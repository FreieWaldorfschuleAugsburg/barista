package de.waldorfaugsburg.barista.payment;

import de.waldorfaugsburg.barista.BaristaApplication;
import de.waldorfaugsburg.barista.mdb.MDBProduct;
import de.waldorfaugsburg.barista.sound.Sound;
import de.waldorfaugsburg.pivot.client.ApiException;
import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.protocol.User;
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
        // Create sentry user
        final User user = new User();
        user.setId(chipId);
        Sentry.setUser(user);

        application.getSoundPlayer().play(Sound.START);

        final MDBProduct product = application.getMdbInterface().awaitProduct();
        if (product == null) {
            application.getMdbInterface().stopSelection();
            application.getSoundPlayer().play(Sound.TIMEOUT);
            log.info("Payment for '{}' timed out", chipId);
            return;
        }

        ISpan currentSpan = Sentry.startTransaction("payment", "mapProduct");
        currentSpan.setData("id", product.productId());
        currentSpan.finish();

        // Get corresponding product barcode
        final Long productBarcode = application.getConfiguration().getProducts().get(product.productId());
        if (productBarcode == null) {
            application.getMdbInterface().cancelPayment();
            application.getSoundPlayer().play(Sound.INVALID_PRODUCT);
            log.error("Payment for '{}' with invalid product id '{}' requested", chipId, product.productId());

            currentSpan.finish(SpanStatus.INVALID_ARGUMENT);
            return;
        }

        currentSpan.setData("barcode", productBarcode);
        currentSpan.finish();

        currentSpan = currentSpan.startChild("checkForServiceUser");

        // Check if is service chip
        if (!application.getConfiguration().getServiceChipId().equals(chipId)) {
            currentSpan.setData("serviceUser", false);
            currentSpan.finish();

            currentSpan = currentSpan.startChild("transaction");
            try {
                application.getPivotClient().getMensaMaxApi().transaction(chipId, application.getConfiguration().getPivot().getKiosk(), productBarcode);

                currentSpan.finish();
            } catch (final ApiException e) {
                application.getMdbInterface().cancelPayment();
                application.getSoundPlayer().play(e.getError() == null ? Sound.UNKNOWN_ERROR : Sound.findByName(e.getError().getCode()));
                log.error("Transaction by '{}' for product '{}' ({}€) failed", chipId, product.productId(), product.money(), e);

                currentSpan.finish(SpanStatus.INTERNAL_ERROR);
                return;
            }
        } else {
            currentSpan.setData("serviceUser", true);
            currentSpan.finish();

            application.getSoundPlayer().play(Sound.SERVICE);
        }

        currentSpan = currentSpan.startChild("confirmation");

        application.getMdbInterface().confirmPayment(product);
        log.info("Successful transaction by '{}' for product '{}' ({}€)", chipId, product.productId(), product.money());

        currentSpan.finish();
    }
}