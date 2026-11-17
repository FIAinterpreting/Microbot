package net.runelite.client.plugins.microbot.autowoodcutter;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Auto Woodcutter",
        description = "Chops the selected tree, banks when the inventory is full, then returns",
        tags = {"woodcutting", "microbot", "skilling"},
        enabledByDefault = false
)
@Slf4j
public class AutoWoodcutterPlugin extends Plugin {

    @Inject
    private AutoWoodcutterScript script;

    @Inject
    private AutoWoodcutterConfig config;

    @Provides
    AutoWoodcutterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoWoodcutterConfig.class);
    }

    @Override
    protected void startUp() {
        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
    }
}
