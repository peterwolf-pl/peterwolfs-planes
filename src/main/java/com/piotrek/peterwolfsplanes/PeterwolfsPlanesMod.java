package com.piotrek.peterwolfsplanes;

import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import com.piotrek.peterwolfsplanes.entity.LargePlaneEntity;
import com.piotrek.peterwolfsplanes.entity.LargeTwinEnginePlaneEntity;
import com.piotrek.peterwolfsplanes.entity.TriplaneEntity;
import com.piotrek.peterwolfsplanes.entity.WaterPlaneEntity;
import com.piotrek.peterwolfsplanes.item.PlaneItem;
import com.piotrek.peterwolfsplanes.network.ParagliderInputPayload;
import com.piotrek.peterwolfsplanes.network.PlaneInputPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.piotrek.peterwolfsplanes.entity.VillagerPilotEntity;
import com.piotrek.peterwolfsplanes.item.SquadronHornItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.piotrek.peterwolfsplanes.entity.MonoplaneEntity;
import com.piotrek.peterwolfsplanes.entity.HelicopterEntity;
import com.piotrek.peterwolfsplanes.item.ParagliderBackpackItem;
import com.piotrek.peterwolfsplanes.network.ParagliderSyncPayload;

public class PeterwolfsPlanesMod implements ModInitializer {
	public static final String MOD_ID = "peterwolfs_planes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ResourceKey<Structure> RUNWAY_STRUCTURE_KEY = ResourceKey.create(Registries.STRUCTURE, id("runway"));
	public static final TagKey<Structure> RUNWAY_STRUCTURES_TAG = TagKey.create(Registries.STRUCTURE, id("runways"));

	public static final ResourceKey<EntityType<?>> PLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("plane"));
	public static final EntityType<PlaneEntity> PLANE_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		PLANE_ENTITY_KEY,
		EntityType.Builder.<PlaneEntity>of(PlaneEntity::new, MobCategory.MISC)
			.sized(2.8f, 1.8f)
			.clientTrackingRange(10)
			.build(PLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> LARGE_PLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("large_plane"));
	public static final EntityType<LargePlaneEntity> LARGE_PLANE_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		LARGE_PLANE_ENTITY_KEY,
		EntityType.Builder.<LargePlaneEntity>of(LargePlaneEntity::new, MobCategory.MISC)
			.sized(3.64f, 2.07f)
			.clientTrackingRange(10)
			.build(LARGE_PLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> LARGE_TWIN_ENGINE_PLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("large_twin_engine_plane"));
	public static final EntityType<LargeTwinEnginePlaneEntity> LARGE_TWIN_ENGINE_PLANE_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		LARGE_TWIN_ENGINE_PLANE_ENTITY_KEY,
		EntityType.Builder.<LargeTwinEnginePlaneEntity>of(LargeTwinEnginePlaneEntity::new, MobCategory.MISC)
			.sized(3.64f, 2.07f)
			.clientTrackingRange(10)
			.build(LARGE_TWIN_ENGINE_PLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> TRIPLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("triplane"));
	public static final EntityType<TriplaneEntity> TRIPLANE_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		TRIPLANE_ENTITY_KEY,
		EntityType.Builder.<TriplaneEntity>of(TriplaneEntity::new, MobCategory.MISC)
			.sized(2.1f, 1.8f)
			.clientTrackingRange(10)
			.build(TRIPLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> WATER_PLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("water_plane"));
	public static final EntityType<WaterPlaneEntity> WATER_PLANE_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		WATER_PLANE_ENTITY_KEY,
		EntityType.Builder.<WaterPlaneEntity>of(WaterPlaneEntity::new, MobCategory.MISC)
			.sized(3.64f, 2.3f)
			.clientTrackingRange(10)
			.build(WATER_PLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> MONOPLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("monoplane"));
	public static final EntityType<MonoplaneEntity> MONOPLANE_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		MONOPLANE_ENTITY_KEY,
		EntityType.Builder.<MonoplaneEntity>of(MonoplaneEntity::new, MobCategory.MISC)
			.sized(2.8f, 1.8f)
			.clientTrackingRange(10)
			.build(MONOPLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> HELICOPTER_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("helicopter"));
	public static final EntityType<HelicopterEntity> HELICOPTER_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		HELICOPTER_ENTITY_KEY,
		EntityType.Builder.<HelicopterEntity>of(HelicopterEntity::new, MobCategory.MISC)
			.sized(3.4f, 2.6f)
			.clientTrackingRange(10)
			.build(HELICOPTER_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> VILLAGER_PILOT_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("villager_pilot"));
	public static final EntityType<VillagerPilotEntity> VILLAGER_PILOT_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		VILLAGER_PILOT_ENTITY_KEY,
		EntityType.Builder.<VillagerPilotEntity>of(VillagerPilotEntity::new, MobCategory.CREATURE)
			.sized(0.6f, 1.95f)
			.clientTrackingRange(10)
			.build(VILLAGER_PILOT_ENTITY_KEY)
	);

	public static final ResourceKey<Item> PLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("plane"));
	public static final PlaneItem PLANE_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		PLANE_ITEM_KEY,
		new PlaneItem(new Item.Properties().setId(PLANE_ITEM_KEY).stacksTo(1), (world) -> new PlaneEntity(PLANE_ENTITY, world))
	);

	public static final ResourceKey<Item> LARGE_PLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("large_plane"));
	public static final PlaneItem LARGE_PLANE_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		LARGE_PLANE_ITEM_KEY,
		new PlaneItem(new Item.Properties().setId(LARGE_PLANE_ITEM_KEY).stacksTo(1), (world) -> new LargePlaneEntity(LARGE_PLANE_ENTITY, world))
	);

	public static final ResourceKey<Item> LARGE_TWIN_ENGINE_PLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("large_twin_engine_plane"));
	public static final PlaneItem LARGE_TWIN_ENGINE_PLANE_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		LARGE_TWIN_ENGINE_PLANE_ITEM_KEY,
		new PlaneItem(new Item.Properties().setId(LARGE_TWIN_ENGINE_PLANE_ITEM_KEY).stacksTo(1), (world) -> new LargeTwinEnginePlaneEntity(LARGE_TWIN_ENGINE_PLANE_ENTITY, world))
	);

	public static final ResourceKey<Item> TRIPLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("triplane"));
	public static final PlaneItem TRIPLANE_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		TRIPLANE_ITEM_KEY,
		new PlaneItem(new Item.Properties().setId(TRIPLANE_ITEM_KEY).stacksTo(1), (world) -> new TriplaneEntity(TRIPLANE_ENTITY, world))
	);

	public static final ResourceKey<Item> WATER_PLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("water_plane"));
	public static final PlaneItem WATER_PLANE_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		WATER_PLANE_ITEM_KEY,
		new PlaneItem(new Item.Properties().setId(WATER_PLANE_ITEM_KEY).stacksTo(1), (world) -> new WaterPlaneEntity(WATER_PLANE_ENTITY, world), true)
	);

	public static final ResourceKey<Item> MONOPLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("monoplane"));
	public static final PlaneItem MONOPLANE_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		MONOPLANE_ITEM_KEY,
		new PlaneItem(new Item.Properties().setId(MONOPLANE_ITEM_KEY).stacksTo(1), (world) -> new MonoplaneEntity(MONOPLANE_ENTITY, world))
	);

	public static final ResourceKey<Item> HELICOPTER_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("helicopter"));
	public static final PlaneItem HELICOPTER_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		HELICOPTER_ITEM_KEY,
		new PlaneItem(new Item.Properties().setId(HELICOPTER_ITEM_KEY).stacksTo(1), (world) -> new HelicopterEntity(HELICOPTER_ENTITY, world))
	);

	public static final ResourceKey<Item> PARAGLIDER_BACKPACK_KEY = ResourceKey.create(Registries.ITEM, id("paraglider_backpack"));
	public static final ParagliderBackpackItem PARAGLIDER_BACKPACK = Registry.register(
		BuiltInRegistries.ITEM,
		PARAGLIDER_BACKPACK_KEY,
		new ParagliderBackpackItem(new Item.Properties().setId(PARAGLIDER_BACKPACK_KEY).stacksTo(1).equippable(EquipmentSlot.CHEST))
	);

	public static final ResourceKey<Item> VILLAGER_PILOT_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, id("villager_pilot_spawn_egg"));
	public static final SpawnEggItem VILLAGER_PILOT_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		VILLAGER_PILOT_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(VILLAGER_PILOT_ENTITY).setId(VILLAGER_PILOT_SPAWN_EGG_KEY))
	);

	public static final ResourceKey<Item> SQUADRON_HORN_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("squadron_horn"));
	public static final SquadronHornItem SQUADRON_HORN = Registry.register(
		BuiltInRegistries.ITEM,
		SQUADRON_HORN_ITEM_KEY,
		new SquadronHornItem(new Item.Properties().setId(SQUADRON_HORN_ITEM_KEY).stacksTo(1))
	);

	public static final SoundEvent PLANE_ENGINE_SOUND = Registry.register(
		BuiltInRegistries.SOUND_EVENT,
		id("entity.plane.engine"),
		SoundEvent.createFixedRangeEvent(id("entity.plane.engine"), 64.0F)
	);
	public static final SoundEvent HELICOPTER_ENGINE_SOUND = Registry.register(
		BuiltInRegistries.SOUND_EVENT,
		id("entity.helicopter.engine"),
		SoundEvent.createFixedRangeEvent(id("entity.helicopter.engine"), 64.0F)
	);

	public static final CreativeModeTab PLANES_GROUP = FabricItemGroup.builder()
		.title(Component.translatable("itemGroup.peterwolfs_planes.group"))
		.displayItems((params, output) -> {
			output.accept(PLANE_ITEM.getDefaultInstance());
			output.accept(LARGE_PLANE_ITEM.getDefaultInstance());
			output.accept(LARGE_TWIN_ENGINE_PLANE_ITEM.getDefaultInstance());
			output.accept(WATER_PLANE_ITEM.getDefaultInstance());
			output.accept(TRIPLANE_ITEM.getDefaultInstance());
			output.accept(MONOPLANE_ITEM.getDefaultInstance());
			output.accept(HELICOPTER_ITEM.getDefaultInstance());
			output.accept(PARAGLIDER_BACKPACK.getDefaultInstance());
			output.accept(VILLAGER_PILOT_SPAWN_EGG.getDefaultInstance());
			output.accept(SQUADRON_HORN.getDefaultInstance());
		})
		.icon(PLANE_ITEM::getDefaultInstance)
		.build();

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("group"), PLANES_GROUP);

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.addAfter(Items.ELYTRA, PARAGLIDER_BACKPACK);
			output.addAfter(PARAGLIDER_BACKPACK, PLANE_ITEM);
			output.addAfter(PLANE_ITEM, LARGE_PLANE_ITEM);
			output.addAfter(LARGE_PLANE_ITEM, LARGE_TWIN_ENGINE_PLANE_ITEM);
			output.addAfter(LARGE_TWIN_ENGINE_PLANE_ITEM, WATER_PLANE_ITEM);
			output.addAfter(WATER_PLANE_ITEM, TRIPLANE_ITEM);
			output.addAfter(TRIPLANE_ITEM, MONOPLANE_ITEM);
			output.addAfter(MONOPLANE_ITEM, HELICOPTER_ITEM);
			output.addAfter(HELICOPTER_ITEM, VILLAGER_PILOT_SPAWN_EGG);
			output.addAfter(VILLAGER_PILOT_SPAWN_EGG, SQUADRON_HORN);
		});

		// Register Entity Attributes for Villager Pilot Mob
		FabricDefaultAttributeRegistry.register(VILLAGER_PILOT_ENTITY, VillagerPilotEntity.createAttributes());

		// Register Networking Payloads
		PayloadTypeRegistry.playC2S().register(PlaneInputPayload.TYPE, PlaneInputPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ParagliderInputPayload.TYPE, ParagliderInputPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ParagliderSyncPayload.TYPE, ParagliderSyncPayload.CODEC);

		// Initialize Paraglider Server Handler + ridge-lift particle viz + commands
		ParagliderHandler.register();
		ParagliderLiftParticles.register();
		PeterwolfsPlanesCommands.register();

		// Handle C2S Inputs (flight + dogfight combat)
		ServerPlayNetworking.registerGlobalReceiver(PlaneInputPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				if (player.getVehicle() instanceof PlaneEntity plane) {
					plane.setThrottle(payload.throttle());
					plane.setRudder(payload.rudder());
					plane.applyCombatInput(payload.combatMode(), payload.fireGuns(), payload.dropBomb());
					if (plane.isHelicopter()) {
						plane.setHelicopterAirborne(payload.helicopterAirborne());
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(ParagliderInputPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> ParagliderHandler.setFlightMode(context.player(), payload.mode()));
		});

		LOGGER.info("Registered Peter Wolf's Planes Mod!");
	}

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
