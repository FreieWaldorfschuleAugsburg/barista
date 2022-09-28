package de.waldorfaugsburg.barista.mensamax;

import lombok.extern.slf4j.Slf4j;
import xyz.cp74.evdev.EventType;
import xyz.cp74.evdev.InputDevice;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class MensaMaxChipReader implements AutoCloseable {

    private static final Map<Integer, Integer> KEY_MAP = new HashMap<>();

    static {
        final List<Integer> orderedKeys = new ArrayList<>();
        orderedKeys.add(KeyEvent.VK_ESCAPE);
        orderedKeys.add(KeyEvent.VK_1);
        orderedKeys.add(KeyEvent.VK_2);
        orderedKeys.add(KeyEvent.VK_3);
        orderedKeys.add(KeyEvent.VK_4);
        orderedKeys.add(KeyEvent.VK_5);
        orderedKeys.add(KeyEvent.VK_6);
        orderedKeys.add(KeyEvent.VK_7);
        orderedKeys.add(KeyEvent.VK_8);
        orderedKeys.add(KeyEvent.VK_9);
        orderedKeys.add(KeyEvent.VK_0);
        orderedKeys.add(KeyEvent.VK_MINUS);
        orderedKeys.add(KeyEvent.VK_EQUALS);
        orderedKeys.add(KeyEvent.VK_BACK_SPACE);
        orderedKeys.add(KeyEvent.VK_TAB);
        orderedKeys.add(KeyEvent.VK_Q);
        orderedKeys.add(KeyEvent.VK_W);
        orderedKeys.add(KeyEvent.VK_E);
        orderedKeys.add(KeyEvent.VK_R);
        orderedKeys.add(KeyEvent.VK_T);
        orderedKeys.add(KeyEvent.VK_Y);
        orderedKeys.add(KeyEvent.VK_U);
        orderedKeys.add(KeyEvent.VK_I);
        orderedKeys.add(KeyEvent.VK_O);
        orderedKeys.add(KeyEvent.VK_P);
        orderedKeys.add(KeyEvent.VK_BRACELEFT);
        orderedKeys.add(KeyEvent.VK_BRACERIGHT);
        orderedKeys.add(KeyEvent.VK_ENTER);
        orderedKeys.add(KeyEvent.VK_CONTROL);
        orderedKeys.add(KeyEvent.VK_A);
        orderedKeys.add(KeyEvent.VK_S);
        orderedKeys.add(KeyEvent.VK_D);
        orderedKeys.add(KeyEvent.VK_F);
        orderedKeys.add(KeyEvent.VK_G);
        orderedKeys.add(KeyEvent.VK_H);
        orderedKeys.add(KeyEvent.VK_J);
        orderedKeys.add(KeyEvent.VK_K);
        orderedKeys.add(KeyEvent.VK_L);
        orderedKeys.add(KeyEvent.VK_SEMICOLON);
        orderedKeys.add(KeyEvent.VK_QUOTE);
        orderedKeys.add(KeyEvent.VK_DEAD_GRAVE);
        orderedKeys.add(KeyEvent.VK_SHIFT);
        orderedKeys.add(KeyEvent.VK_BACK_SLASH);
        orderedKeys.add(KeyEvent.VK_Z);
        orderedKeys.add(KeyEvent.VK_X);
        orderedKeys.add(KeyEvent.VK_C);
        orderedKeys.add(KeyEvent.VK_V);
        orderedKeys.add(KeyEvent.VK_B);
        orderedKeys.add(KeyEvent.VK_N);
        orderedKeys.add(KeyEvent.VK_M);

        for (int i = 0; i < orderedKeys.size(); i++) {
            KEY_MAP.put(i + 1, orderedKeys.get(i));
        }
    }

    private final InputDevice device;
    private final StringBuilder builder;

    private long lastEventAt;

    public MensaMaxChipReader(final String monitorPath) {
        this.device = new InputDevice(monitorPath);
        this.builder = new StringBuilder();

        final InputDevice inputDevice = new InputDevice(monitorPath);
        inputDevice.onEvent(event -> {
            final long lastEventAt = this.lastEventAt;
            this.lastEventAt = System.currentTimeMillis();

            // Drop all collected characters if last event is longer than one second ago
            if (System.currentTimeMillis() - lastEventAt >= TimeUnit.SECONDS.toMillis(1)) {
                builder.setLength(0);
            }

            if (event.getType() == EventType.KEY.getId() && event.getValue() == 1) {
                // Get corresponding AWT key code
                final Integer keyCode = KEY_MAP.get(event.getCode());

                // Check that pressed key isn't a control key
                if (keyCode != KeyEvent.VK_ENTER && keyCode != KeyEvent.VK_SHIFT) {
                    builder.append(KeyEvent.getKeyText(keyCode));
                    // Notify builder of change
                    synchronized (builder) {
                        builder.notify();
                    }
                }
            }
        });
        inputDevice.start();
    }

    public String awaitChip() throws InterruptedException {
        // Clear builder on initialize
        builder.setLength(0);

        // Block current thread until builder is full
        while (builder.length() < 10) {
            synchronized (builder) {
                builder.wait();
            }
        }

        // Saving chip
        final String chip = builder.toString();
        builder.setLength(0);
        return chip;
    }

    @Override
    public void close() {
        // Closing device
        device.finish();
    }
}
