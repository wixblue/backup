package com.wixblue.backup.service;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Strömbrytare för backup. När den är avstängd körs ingen backup, men jämförelse fungerar som vanligt.
 * Startläget sätts med {@code backup.enabled} (standard: påslagen). Läget sparas bara i minnet,
 * så efter en omstart gäller startläget igen.
 */
@Component
public class BackupSwitch {

    private final AtomicBoolean enabled;

    public BackupSwitch(@Value("${backup.enabled:true}") boolean enabled) {
        this.enabled = new AtomicBoolean(enabled);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void enable() {
        enabled.set(true);
    }

    public void disable() {
        enabled.set(false);
    }
}
