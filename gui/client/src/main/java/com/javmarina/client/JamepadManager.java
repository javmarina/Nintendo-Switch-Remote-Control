package com.javmarina.client;

import com.javmarina.client.services.DefaultJamepadService;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerManager;

import java.util.ArrayList;


/**
 * Class that manages running instances of {@link DefaultJamepadService} and enables/disables SDL native library.
 */
public final class JamepadManager {

    private static final int MAX_CONTROLLERS = 10;

    private static final ControllerManager manager = new ControllerManager(MAX_CONTROLLERS);

    static {
        manager.initSDLGamepad();
        Runtime.getRuntime().addShutdownHook(new Thread(manager::quitSDLGamepad));
    }

    public static void update() {
        manager.update();
    }

    public static ArrayList<DefaultJamepadService> getAvailableJamepadServices() {
        manager.update();
        final int size = manager.getNumControllers();
        final ArrayList<DefaultJamepadService> services = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            try {
                // TODO: even though getNumControllers() is > 0, this method could throw an exception
                final ControllerIndex controllerIndex = manager.getControllerIndex(i);
                if (controllerIndex.isConnected()) {
                    services.add(DefaultJamepadService.fromControllerIndex(controllerIndex));
                }
            } catch (final ArrayIndexOutOfBoundsException e) {
            }
        }
        return services;
    }
}
