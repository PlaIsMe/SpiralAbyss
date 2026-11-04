package com.pla.spiral_abyss;
import com.pla.spiral_abyss.config.GearConfig;
import com.pla.spiral_abyss.init.*;
public final class SpiralAbyss {
 public static final String MOD_ID="spiral_abyss";
 public static void init(){
  GearConfig.prepare();
  EntityInit.ENTITY_TYPES.register(); BlockInit.BLOCKS.register(); BlockEntityInit.BLOCK_ENTITIES.register();
  ItemInit.ITEMS.register(); CreativeTabInit.TABS.register(); SoundInit.SOUNDS.register();
 }
}
