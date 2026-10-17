package com.piotrek.peterwolfsplanes;

import com.piotrek.peterwolfsplanes.entity.HelicopterEntity;
import com.piotrek.peterwolfsplanes.entity.LargePlaneEntity;
import com.piotrek.peterwolfsplanes.entity.LargeTwinEnginePlaneEntity;
import com.piotrek.peterwolfsplanes.entity.MonoplaneEntity;
import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import com.piotrek.peterwolfsplanes.entity.TriplaneEntity;
import com.piotrek.peterwolfsplanes.entity.VillagerPilotEntity;
import com.piotrek.peterwolfsplanes.entity.WaterPlaneEntity;
import com.piotrek.peterwolfsplanes.item.ParagliderBackpackItem;
import com.piotrek.peterwolfsplanes.item.PlaneItem;
import com.piotrek.peterwolfsplanes.item.SquadronHornItem;
import com.piotrek.peterwolfsplanes.network.ParagliderInputPayload;
import com.piotrek.peterwolfsplanes.network.ParagliderSyncPayload;
import com.piotrek.peterwolfsplanes.network.PlaneInputPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(PeterwolfsPlanesMod.MOD_ID)
public class PeterwolfsPlanesMod {
	public static final String MOD_ID = "peterwolfs_planes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ResourceKey<Structure> RUNWAY_STRUCTURE_KEY = ResourceKey.create(Registries.STRUCTURE, id("runway"));
	public static final TagKey<Structure> RUNWAY_STRUCTURES_TAG = TagKey.create(Registries.STRUCTURE, id("runways"));

	public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(MOD_ID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
	public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

	public static final DeferredHolder<SoundEvent, SoundEvent> PLANE_ENGINE_SOUND = SOUND_EVENTS.register(
		"engine.plane",
		() -> SoundEvent.createVariableRangeEvent(id("engine.plane"))
	);
	public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_ENGINE_SOUND = SOUND_EVENTS.register(
		"engine.helicopter",
		() -> SoundEvent.createVariableRangeEvent(id("engine.helicopter"))
	);

	public static final ResourceKey<EntityType<?>> PLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("plane"));
	public static final DeferredHolder<EntityType<?>, EntityType<PlaneEntity>> PLANE_ENTITY = ENTITY_TYPES.register(
		"plane",
		() -> EntityType.Builder.<PlaneEntity>of(PlaneEntity::new, MobCategory.MISC)
			.sized(2.8f, 1.8f)
			.clientTrackingRange(10)
			.build(PLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> LARGE_PLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("large_plane"));
	public static final DeferredHolder<EntityType<?>, EntityType<LargePlaneEntity>> LARGE_PLANE_ENTITY = ENTITY_TYPES.register(
		"large_plane",
		() -> EntityType.Builder.<LargePlaneEntity>of(LargePlaneEntity::new, MobCategory.MISC)
			.sized(3.64f, 2.07f)
			.clientTrackingRange(10)
			.build(LARGE_PLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> LARGE_TWIN_ENGINE_PLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("large_twin_engine_plane"));
	public static final DeferredHolder<EntityType<?>, EntityType<LargeTwinEnginePlaneEntity>> LARGE_TWIN_ENGINE_PLANE_ENTITY = ENTITY_TYPES.register(
		"large_twin_engine_plane",
		() -> EntityType.Builder.<LargeTwinEnginePlaneEntity>of(LargeTwinEnginePlaneEntity::new, MobCategory.MISC)
			.sized(3.64f, 2.07f)
			.clientTrackingRange(10)
			.build(LARGE_TWIN_ENGINE_PLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> TRIPLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("triplane"));
	public static final DeferredHolder<EntityType<?>, EntityType<TriplaneEntity>> TRIPLANE_ENTITY = ENTITY_TYPES.register(
		"triplane",
		() -> EntityType.Builder.<TriplaneEntity>of(TriplaneEntity::new, MobCategory.MISC)
			.sized(2.1f, 1.8f)
			.clientTrackingRange(10)
			.build(TRIPLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> WATER_PLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("water_plane"));
	public static final DeferredHolder<EntityType<?>, EntityType<WaterPlaneEntity>> WATER_PLANE_ENTITY = ENTITY_TYPES.register(
		"water_plane",
		() -> EntityType.Builder.<WaterPlaneEntity>of(WaterPlaneEntity::new, MobCategory.MISC)
			.sized(3.64f, 2.3f)
			.clientTrackingRange(10)
			.build(WATER_PLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> MONOPLANE_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("monoplane"));
	public static final DeferredHolder<EntityType<?>, EntityType<MonoplaneEntity>> MONOPLANE_ENTITY = ENTITY_TYPES.register(
		"monoplane",
		() -> EntityType.Builder.<MonoplaneEntity>of(MonoplaneEntity::new, MobCategory.MISC)
			.sized(2.8f, 1.8f)
			.clientTrackingRange(10)
			.build(MONOPLANE_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> HELICOPTER_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("helicopter"));
	public static final DeferredHolder<EntityType<?>, EntityType<HelicopterEntity>> HELICOPTER_ENTITY = ENTITY_TYPES.register(
		"helicopter",
		() -> EntityType.Builder.<HelicopterEntity>of(HelicopterEntity::new, MobCategory.MISC)
			.sized(2.6f, 2.2f)
			.clientTrackingRange(10)
			.build(HELICOPTER_ENTITY_KEY)
	);

	public static final ResourceKey<EntityType<?>> VILLAGER_PILOT_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("villager_pilot"));
	public static final DeferredHolder<EntityType<?>, EntityType<VillagerPilotEntity>> VILLAGER_PILOT_ENTITY = ENTITY_TYPES.register(
		"villager_pilot",
		() -> EntityType.Builder.<VillagerPilotEntity>of(VillagerPilotEntity::new, MobCategory.CREATURE)
			.sized(0.6f, 1.95f)
			.clientTrackingRange(10)
			.build(VILLAGER_PILOT_ENTITY_KEY)
	);

	public static final ResourceKey<Item> PLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("plane"));
	public static final DeferredItem<PlaneItem> PLANE_ITEM = ITEMS.register(
		"plane",
		() -> new PlaneItem(new Item.Properties().setId(PLANE_ITEM_KEY).stacksTo(1), (world) -> new PlaneEntity(PLANE_ENTITY.get(), world))
	);

	public static final ResourceKey<Item> LARGE_PLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("large_plane"));
	public static final DeferredItem<PlaneItem> LARGE_PLANE_ITEM = ITEMS.register(
		"large_plane",
		() -> new PlaneItem(new Item.Properties().setId(LARGE_PLANE_ITEM_KEY).stacksTo(1), (world) -> new LargePlaneEntity(LARGE_PLANE_ENTITY.get(), world))
	);

	public static final ResourceKey<Item> LARGE_TWIN_ENGINE_PLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("large_twin_engine_plane"));
	public static final DeferredItem<PlaneItem> LARGE_TWIN_ENGINE_PLANE_ITEM = ITEMS.register(
		"large_twin_engine_plane",
		() -> new PlaneItem(new Item.Properties().setId(LARGE_TWIN_ENGINE_PLANE_ITEM_KEY).stacksTo(1), (world) -> new LargeTwinEnginePlaneEntity(LARGE_TWIN_ENGINE_PLANE_ENTITY.get(), world))
	);

	public static final ResourceKey<Item> TRIPLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("triplane"));
	public static final DeferredItem<PlaneItem> TRIPLANE_ITEM = ITEMS.register(
		"triplane",
		() -> new PlaneItem(new Item.Properties().setId(TRIPLANE_ITEM_KEY).stacksTo(1), (world) -> new TriplaneEntity(TRIPLANE_ENTITY.get(), world))
	);

	public static final ResourceKey<Item> WATER_PLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("water_plane"));
	public static final DeferredItem<PlaneItem> WATER_PLANE_ITEM = ITEMS.register(
		"water_plane",
		() -> new PlaneItem(new Item.Properties().setId(WATER_PLANE_ITEM_KEY).stacksTo(1), (world) -> new WaterPlaneEntity(WATER_PLANE_ENTITY.get(), world), true)
	);

	public static final ResourceKey<Item> MONOPLANE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("monoplane"));
	public static final DeferredItem<PlaneItem> MONOPLANE_ITEM = ITEMS.register(
		"monoplane",
		() -> new PlaneItem(new Item.Properties().setId(MONOPLANE_ITEM_KEY).stacksTo(1), (world) -> new MonoplaneEntity(MONOPLANE_ENTITY.get(), world))
	);

	public static final ResourceKey<Item> HELICOPTER_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("helicopter"));
	public static final DeferredItem<PlaneItem> HELICOPTER_ITEM = ITEMS.register(
		"helicopter",
		() -> new PlaneItem(new Item.Properties().setId(HELICOPTER_ITEM_KEY).stacksTo(1), (world) -> new HelicopterEntity(HELICOPTER_ENTITY.get(), world))
	);

	public static final ResourceKey<Item> PARAGLIDER_BACKPACK_KEY = ResourceKey.create(Registries.ITEM, id("paraglider_backpack"));
	public static final DeferredItem<ParagliderBackpackItem> PARAGLIDER_BACKPACK = ITEMS.register(
		"paraglider_backpack",
		() -> new ParagliderBackpackItem(new Item.Properties().setId(PARAGLIDER_BACKPACK_KEY).stacksTo(1).equippable(EquipmentSlot.CHEST))
	);

	public static final ResourceKey<Item> VILLAGER_PILOT_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, id("villager_pilot_spawn_egg"));
	public static final DeferredItem<SpawnEggItem> VILLAGER_PILOT_SPAWN_EGG = ITEMS.register(
		"villager_pilot_spawn_egg",
		() -> new SpawnEggItem(new Item.Properties().spawnEgg(VILLAGER_PILOT_ENTITY.get()).setId(VILLAGER_PILOT_SPAWN_EGG_KEY))
	);

	public static final ResourceKey<Item> SQUADRON_HORN_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("squadron_horn"));
	public static final DeferredItem<SquadronHornItem> SQUADRON_HORN = ITEMS.register(
		"squadron_horn",
		() -> new SquadronHornItem(new Item.Properties().setId(SQUADRON_HORN_ITEM_KEY).stacksTo(1))
	);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PLANES_GROUP = CREATIVE_TABS.register(
		"group",
		() -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.peterwolfs_planes.group"))
			.displayItems((params, output) -> {
				output.accept(PLANE_ITEM.get());
				output.accept(LARGE_PLANE_ITEM.get());
				output.accept(LARGE_TWIN_ENGINE_PLANE_ITEM.get());
				output.accept(WATER_PLANE_ITEM.get());
				output.accept(TRIPLANE_ITEM.get());
				output.accept(MONOPLANE_ITEM.get());
				output.accept(HELICOPTER_ITEM.get());
				output.accept(PARAGLIDER_BACKPACK.get());
				output.accept(VILLAGER_PILOT_SPAWN_EGG.get());
				output.accept(SQUADRON_HORN.get());
			})
			.icon(() -> PLANE_ITEM.get().getDefaultInstance())
			.build()
	);

	public PeterwolfsPlanesMod(IEventBus modBus) {
		ENTITY_TYPES.register(modBus);
		ITEMS.register(modBus);
		CREATIVE_TABS.register(modBus);
		SOUND_EVENTS.register(modBus);
		modBus.addListener(PeterwolfsPlanesMod::addCreative);
		modBus.addListener(PeterwolfsPlanesMod::registerAttributes);
		modBus.addListener(PeterwolfsPlanesMod::registerPayloads);
		NeoForge.EVENT_BUS.register(ServerEvents.class);
		LOGGER.info("Registered Peter Wolf's Planes Mod!");
	}

	private static void addCreative(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES) {
			return;
		}
		event.insertAfter(Items.ELYTRA.getDefaultInstance(), PARAGLIDER_BACKPACK.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		event.insertAfter(PARAGLIDER_BACKPACK.get().getDefaultInstance(), PLANE_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		event.insertAfter(PLANE_ITEM.get().getDefaultInstance(), LARGE_PLANE_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		event.insertAfter(LARGE_PLANE_ITEM.get().getDefaultInstance(), LARGE_TWIN_ENGINE_PLANE_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		event.insertAfter(LARGE_TWIN_ENGINE_PLANE_ITEM.get().getDefaultInstance(), WATER_PLANE_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		event.insertAfter(WATER_PLANE_ITEM.get().getDefaultInstance(), TRIPLANE_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		event.insertAfter(TRIPLANE_ITEM.get().getDefaultInstance(), MONOPLANE_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		event.insertAfter(MONOPLANE_ITEM.get().getDefaultInstance(), HELICOPTER_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		event.insertAfter(HELICOPTER_ITEM.get().getDefaultInstance(), VILLAGER_PILOT_SPAWN_EGG.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		event.insertAfter(VILLAGER_PILOT_SPAWN_EGG.get().getDefaultInstance(), SQUADRON_HORN.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
	}

	private static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(VILLAGER_PILOT_ENTITY.get(), VillagerPilotEntity.createAttributes().build());
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playToServer(PlaneInputPayload.TYPE, PlaneInputPayload.CODEC, (payload, context) -> {
			if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof PlaneEntity plane) {
				plane.setThrottle(payload.throttle());
				plane.setRudder(payload.rudder());
				plane.setHelicopterAirborne(payload.helicopterAirborne());
				plane.applyCombatInput(payload.combatMode(), payload.fireGuns(), payload.dropBomb());
			}
		});
		registrar.playToServer(ParagliderInputPayload.TYPE, ParagliderInputPayload.CODEC, (payload, context) -> {
			if (context.player() instanceof ServerPlayer player) {
				ParagliderHandler.setFlightMode(player, payload.mode());
			}
		});
		registrar.playToClient(ParagliderSyncPayload.TYPE, ParagliderSyncPayload.CODEC);
	}

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static final class ServerEvents {
		private ServerEvents() {
		}

		@SubscribeEvent
		public static void onServerTick(ServerTickEvent.Post event) {
			ParagliderHandler.tick(event.getServer());
			ParagliderLiftParticles.tick(event.getServer());
		}

		@SubscribeEvent
		public static void onRegisterCommands(RegisterCommandsEvent event) {
			PeterwolfsPlanesCommands.register(event.getDispatcher());
		}
	}
}
