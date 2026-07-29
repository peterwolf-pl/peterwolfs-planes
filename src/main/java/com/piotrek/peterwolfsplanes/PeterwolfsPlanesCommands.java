package com.piotrek.peterwolfsplanes;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class PeterwolfsPlanesCommands {
	private PeterwolfsPlanesCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("planes")
				.then(literal("liftparticles")
					.executes(PeterwolfsPlanesCommands::toggleLiftParticles)
					.then(argument("enabled", BoolArgumentType.bool())
						.executes(PeterwolfsPlanesCommands::setLiftParticles)))
			);

			// Short alias
			dispatcher.register(literal("liftparticles")
				.executes(PeterwolfsPlanesCommands::toggleLiftParticles)
				.then(argument("enabled", BoolArgumentType.bool())
					.executes(PeterwolfsPlanesCommands::setLiftParticles)));
		});
	}

	private static int toggleLiftParticles(CommandContext<CommandSourceStack> context) {
		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (Exception e) {
			context.getSource().sendFailure(Component.translatable("command.peterwolfs_planes.players_only"));
			return 0;
		}

		boolean now = ParagliderLiftParticles.toggle(player);
		context.getSource().sendSuccess(
			() -> Component.translatable(now
				? "command.peterwolfs_planes.liftparticles_on"
				: "command.peterwolfs_planes.liftparticles_off"),
			false
		);
		return 1;
	}

	private static int setLiftParticles(CommandContext<CommandSourceStack> context) {
		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (Exception e) {
			context.getSource().sendFailure(Component.translatable("command.peterwolfs_planes.players_only"));
			return 0;
		}

		boolean enabled = BoolArgumentType.getBool(context, "enabled");
		ParagliderLiftParticles.setEnabled(player, enabled);
		context.getSource().sendSuccess(
			() -> Component.translatable(enabled
				? "command.peterwolfs_planes.liftparticles_on"
				: "command.peterwolfs_planes.liftparticles_off"),
			false
		);
		return 1;
	}
}
