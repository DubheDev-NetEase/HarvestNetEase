package dev.dubhe.harvest;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Harvest.MOD_ID)
public class Harvest {
    public static final String MOD_ID = "harvest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Harvest(IEventBus modEventBus, ModContainer modContainer) {
    }
}
