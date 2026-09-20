package org.stht.hastemod.client.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.stht.hastemod.HasteMod;

@Environment(EnvType.CLIENT)
public final class HasteConfigScreen {
    private HasteConfigScreen() {}

    public static Screen build(Screen parent) {
        HasteConfig cfg = HasteConfig.get();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal(HasteMod.MOD_NAME))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("text.hastemod.category.breaking"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("text.hastemod.group.shape"))
                                .option(Option.<HasteConfig.Shape>createBuilder()
                                        .name(Component.translatable("text.hastemod.option.shape"))
                                        .description(OptionDescription.of(
                                                Component.translatable("text.hastemod.option.shape.tooltip")))
                                        .binding(HasteConfig.Shape.CUBE,
                                                () -> cfg.shape,
                                                v -> cfg.shape = v)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(HasteConfig.Shape.class))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("text.hastemod.option.radius"))
                                        .description(OptionDescription.of(
                                                Component.translatable("text.hastemod.option.radius.tooltip")))
                                        .binding(4,
                                                () -> cfg.radius,
                                                v -> cfg.radius = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(1, 16)
                                                .step(1))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("text.hastemod.group.throttle"))
                                .description(OptionDescription.of(
                                        Component.translatable("text.hastemod.group.throttle.tooltip")))
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("text.hastemod.option.blocks_per_tick"))
                                        .description(OptionDescription.of(
                                                Component.translatable("text.hastemod.option.blocks_per_tick.tooltip")))
                                        .binding(16,
                                                () -> cfg.blocksPerTick,
                                                v -> cfg.blocksPerTick = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(1, 256)
                                                .step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("text.hastemod.option.tick_delay"))
                                        .description(OptionDescription.of(
                                                Component.translatable("text.hastemod.option.tick_delay.tooltip")))
                                        .binding(2,
                                                () -> cfg.tickDelay,
                                                v -> cfg.tickDelay = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(0, 200)
                                                .step(1))
                                        .build())
                                .build())
                        .build())
                .save(cfg::save)
                .build()
                .generateScreen(parent);
    }
}
