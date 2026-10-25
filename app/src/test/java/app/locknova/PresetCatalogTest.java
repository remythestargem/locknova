package app.locknova;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import app.locknova.core.Preset;
import app.locknova.core.PresetCatalog;

import java.util.HashSet;
import java.util.Set;

public class PresetCatalogTest {
    @Test public void builtInsAreDistinctAndHaveSupportedThemes() {
        assertEquals(5, PresetCatalog.builtIns().size());
        Set<String> names = new HashSet<>();
        for (Preset preset : PresetCatalog.builtIns()) {
            assertFalse(preset.name.trim().isEmpty());
            assertFalse("Preset names must be unique", names.contains(preset.name));
            names.add(preset.name);
            assertNotEquals(0, preset.accent);
            assertEquals(true, "dark".equals(preset.theme) || "light".equals(preset.theme));
        }
    }
}
