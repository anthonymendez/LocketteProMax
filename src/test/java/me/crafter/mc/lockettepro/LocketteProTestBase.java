package me.crafter.mc.lockettepro;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

public class LocketteProTestBase {

    protected ServerMock server;
    protected LockettePro plugin;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(LockettePro.class);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }
}
