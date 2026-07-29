package com.piotrek.peterwolfsplanes;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.List;

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
				.then(literal("kit")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(PeterwolfsPlanesCommands::kitSelf)
					.then(argument("targets", EntityArgument.players())
						.executes(PeterwolfsPlanesCommands::kitTargets)))
			);

			// Short alias
			dispatcher.register(literal("liftparticles")
				.executes(PeterwolfsPlanesCommands::toggleLiftParticles)
				.then(argument("enabled", BoolArgumentType.bool())
					.executes(PeterwolfsPlanesCommands::setLiftParticles)));

			dispatcher.register(literal("planekit")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(PeterwolfsPlanesCommands::kitSelf)
				.then(argument("targets", EntityArgument.players())
					.executes(PeterwolfsPlanesCommands::kitTargets)));
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

	private static int kitSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		giveStarterKit(player);
		context.getSource().sendSuccess(
			() -> Component.translatable("command.peterwolfs_planes.kit_given", player.getDisplayName()),
			true
		);
		return 1;
	}

	private static int kitTargets(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
		for (ServerPlayer player : targets) {
			giveStarterKit(player);
		}
		int n = targets.size();
		context.getSource().sendSuccess(
			() -> Component.translatable("command.peterwolfs_planes.kit_given_many", n),
			true
		);
		return n;
	}

	/** Every aircraft + paraglider + squadron extras + TNT for bombs. */
	public static void giveStarterKit(ServerPlayer player) {
		List<ItemStack> stacks = List.of(
			new ItemStack(PeterwolfsPlanesMod.PLANE_ITEM),
			new ItemStack(PeterwolfsPlanesMod.LARGE_PLANE_ITEM),
			new ItemStack(PeterwolfsPlanesMod.LARGE_TWIN_ENGINE_PLANE_ITEM),
			new ItemStack(PeterwolfsPlanesMod.TRIPLANE_ITEM),
			new ItemStack(PeterwolfsPlanesMod.WATER_PLANE_ITEM),
			new ItemStack(PeterwolfsPlanesMod.MONOPLANE_ITEM),
			new ItemStack(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK),
			new ItemStack(PeterwolfsPlanesMod.SQUADRON_HORN),
			new ItemStack(PeterwolfsPlanesMod.VILLAGER_PILOT_SPAWN_EGG, 2),
			new ItemStack(Items.TNT, 16)
		);
		for (ItemStack stack : stacks) {
			if (!player.getInventory().add(stack.copy())) {
				player.drop(stack.copy(), false);
			}
		}
		player.sendOverlayMessage(Component.translatable("command.peterwolfs_planes.kit_received"));
	}
}
