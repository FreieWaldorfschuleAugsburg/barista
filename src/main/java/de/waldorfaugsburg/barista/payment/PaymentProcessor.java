package de.waldorfaugsburg.barista.payment;

import de.waldorfaugsburg.barista.BaristaApplication;
import de.waldorfaugsburg.barista.configuration.BaristaConfiguration;
import de.waldorfaugsburg.barista.mdb.MDBProduct;
import de.waldorfaugsburg.barista.sound.Sound;
import de.waldorfaugsburg.pivot.client.ApiException;
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
    public void close() throws Exception {
        listenerThread.interrupt();
    }

    private void startPayment(final String chipId) throws Exception {
        application.getSoundPlayer().play(Sound.START);

        final MDBProduct product = application.getMdbInterface().awaitProduct();
        if (product == null) {
            application.getMdbInterface().stopSelection();

            application.getSoundPlayer().play(Sound.TIMEOUT);
            log.info("Payment for '{}' timed out!", chipId);
            return;
        }

        final BaristaConfiguration.ProductConfiguration productConfiguration = application.getConfiguration().getProducts().get(product.productId());
        if (productConfiguration == null) {
            application.getMdbInterface().cancelPayment();

            application.getSoundPlayer().play(Sound.INVALID_PRODUCT);
            log.error("Payment for invalid product id '{}' requested!", product.productId());
            return;
        }

        try {
            if (application.getConfiguration().getServiceChipId().equals(chipId)) {
                application.getSoundPlayer().play(Sound.SERVICE);
            } else {
                application.getPivotClient().getMensaMaxApi().transaction(chipId, application.getConfiguration().getPivot().getKiosk(), productConfiguration.getBarcode(), 1, productConfiguration.isRestricted());
            }

            application.getMdbInterface().confirmPayment(product);

            log.info("Successful transaction by '{}' for product '{}' ({}€)", chipId, product.productId(), product.money());
        } catch (final ApiException e) {
            application.getMdbInterface().cancelPayment();
            application.getSoundPlayer().play(e.getError() == null ? Sound.UNKNOWN_ERROR : Sound.findByName(e.getError().getCode()));
            log.error("An error occurred while performing transaction by '{}' for product '{}' ({}€)", chipId, product.productId(), product.money(), e);
        }
    }
}