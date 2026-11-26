package com.wixblue.backup.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BackupSwitchTest {

    @Test
    void startsInTheGivenState() {
        assertThat(new BackupSwitch(true).isEnabled()).isTrue();
        assertThat(new BackupSwitch(false).isEnabled()).isFalse();
    }

    @Test
    void canBeDisabledAndEnabledAgain() {
        BackupSwitch backupSwitch = new BackupSwitch(true);

        backupSwitch.disable();
        assertThat(backupSwitch.isEnabled()).isFalse();

        backupSwitch.enable();
        assertThat(backupSwitch.isEnabled()).isTrue();
    }
}
