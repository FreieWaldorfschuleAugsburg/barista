package de.waldorfaugsburg.barista.mdb;

import com.pi4j.io.serial.*;
import de.waldorfaugsburg.barista.BaristaApplication;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
public final class MDBInterface implements AutoCloseable {

    private final BaristaApplication application;
    private final Serial serial;
    private final Function<Long, Boolean> waitFunction;
    private final AtomicReference<MDBProduct> atomicProduct;

    public MDBInterface(final BaristaApplication application) throws IOException, InterruptedException {
        this.application = application;
        this.serial = SerialFactory.createInstance();
        this.waitFunction = startMillis -> System.currentTimeMillis() - startMillis >= application.getConfiguration().getMdb().getTimeoutMillis();
        this.atomicProduct = new AtomicReference<>(null);

        final String defaultPort = SerialPort.getDefaultPort();
        serial.open(new SerialConfig().device(defaultPort).baud(Baud._115200).dataBits(DataBits._8).parity(Parity.NONE).stopBits(StopBits._1));

        log.info("Serial connection opened on port '{}'", defaultPort);

        serial.addListener(event -> {
            try {
                final String data = event.getAsciiString();
                handleIncomingData(data);
            } catch (final IOException e) {
                log.info("An error occurred while handling data", e);
            }
        });

        // Sending version command
        send("V");

        // Enabling "cashless slave" mode
        send("C", "1");
    }

    @Override
    public void close() throws Exception {
        serial.close();
    }

    public MDBProduct awaitProduct() throws InterruptedException {
        // Initiate payment
        send("C", "START", Integer.toString(application.getConfiguration().getMdb().getStartMoney()));

        // Start timeout watcher thread
        final long startMillis = System.currentTimeMillis();
        final Thread watcherThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                if (waitFunction.apply(startMillis)) {
                    synchronized (atomicProduct) {
                        atomicProduct.notify();
                    }
                    break;
                }
            }
        });
        watcherThread.start();

        while (atomicProduct.get() == null) {
            if (waitFunction.apply(startMillis)) return null;

            synchronized (atomicProduct) {
                try {
                    atomicProduct.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        final MDBProduct productId = atomicProduct.get();
        atomicProduct.set(null);
        return productId;
    }

    public void stopSelection() {
        send("C", "STOP");
    }

    public void confirmPayment(final MDBProduct product) {
        send("C", "VEND", Double.toString(product.money()));
    }

    public void cancelPayment() {
        send("C", "VEND", "-1");
    }

    private void handleIncomingData(final String data) {
        final String[] parsedData = parseData(data);

        // Reading incoming data
        if (parsedData[0].equals("c") && parsedData[1].equals("STATUS") && parsedData[2].equals("VEND")) {
            final double money = Double.parseDouble(parsedData[3].trim());
            final int productId = Integer.parseInt(parsedData[4].trim());

            atomicProduct.set(new MDBProduct(productId, money));
            synchronized (atomicProduct) {
                atomicProduct.notify();
            }
        } else if (parsedData[0].equals("v")) {
            log.info("Machine is using MDB version: " + parsedData[1]);
        }
    }

    private void send(final String... args) {
        if (serial.isClosed()) return;

        try {
            serial.writeln(String.join(",", args));
        } catch (final IOException e) {
            log.error("An error occurred while sending data", e);
        }
    }

    private String[] parseData(final String data) {
        return data.split(",");
    }
}
