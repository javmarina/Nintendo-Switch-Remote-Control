package com.javmarina.client;

import com.javmarina.client.services.DefaultJamepadService;
import com.studiohartman.jamepad.ControllerManager;

import java.util.HashSet;
import java.util.Set;


/**
 * Class that manages running instances of {@link DefaultJamepadService} and enables/disables SDL library on the go.
 * It also provides a reference to the global {@link ControllerManager}, which is the way to interact with Jamepad
 * library.
 */
public class JamepadManager {

    private static final int MAX_CONTROLLERS = 10;

    private static final Set<DefaultJamepadService> pool = new HashSet<>(MAX_CONTROLLERS);
    static final ControllerManager manager = new ControllerManager(MAX_CONTROLLERS);

    public static void addService(final DefaultJamepadService service) {
        if (pool.isEmpty()) {
            manager.initSDLGamepad();
        }
        pool.add(service);
    }

    public static void removeService(final DefaultJamepadService service) {
        pool.remove(service);
        if (pool.isEmpty()) {
            manager.quitSDLGamepad();
        }
    }

    public static void update() {
        manager.update();
    }
}
