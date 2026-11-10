package dev.customizablehud.client.hud;

import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.format.ArmorSlotSnapshot;
import dev.customizablehud.format.EffectCategoryKind;
import dev.customizablehud.format.EffectOrderer;
import dev.customizablehud.format.EffectSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LiveDataCollector {
	private LiveDataCollector() {
	}

	public record ArmorRenderEntry(ArmorSlotSnapshot snapshot, ItemStack stack, ArmorSlotKind kind) {
	}

	public record EffectRenderEntry(EffectSnapshot snapshot, Holder<MobEffect> holder) {
	}

	public static float currentTicksPerSecond(Minecraft client) {
		if (client != null && client.level != null) {
			return Math.max(1.0f, client.level.tickRateManager().tickrate());
		}
		return 20.0f;
	}

	public static List<ArmorRenderEntry> collectArmorEntries(Minecraft client, ArmorConfig config, boolean previewFallback) {
		List<ArmorSlotKind> order = config.activeSlotOrder();
		Player player = client != null ? client.player : null;
		if (player != null) {
			List<ArmorRenderEntry> liveEntries = new ArrayList<>(order.size());
			boolean anyPresent = false;
			for (ArmorSlotKind kind : order) {
				ItemStack stack = player.getItemBySlot(toEquipmentSlot(kind));
				ArmorSlotSnapshot snapshot = snapshotFromStack(kind, stack);
				if (snapshot.present()) {
					anyPresent = true;
				}
				liveEntries.add(new ArmorRenderEntry(snapshot, stack, kind));
			}
			if (!previewFallback || anyPresent) {
				return liveEntries;
			}
		}

		if (!previewFallback) {
			return List.of();
		}
		return createPreviewArmorEntries(order);
	}

	public static List<EffectRenderEntry> collectEffectEntries(Minecraft client, EffectsConfig config, boolean previewFallback) {
		Player player = client != null ? client.player : null;
		if (player != null) {
			Collection<MobEffectInstance> active = player.getActiveEffects();
			if (!active.isEmpty()) {
				List<EffectRenderEntry> ordered = orderFromInstances(active, config);
				if (!ordered.isEmpty() || !previewFallback) {
					return ordered;
				}
			} else if (!previewFallback) {
				return List.of();
			}
		}

		if (!previewFallback) {
			return List.of();
		}
		return createPreviewEffectEntries(config);
	}

	private static List<EffectRenderEntry> orderFromInstances(Collection<MobEffectInstance> instances, EffectsConfig config) {
		List<EffectSnapshot> snapshots = new ArrayList<>(instances.size());
		Map<EffectSnapshot, Holder<MobEffect>> holderMap = new IdentityHashMap<>();
		for (MobEffectInstance instance : instances) {
			if (instance == null || instance.getEffect() == null) {
				continue;
			}
			EffectSnapshot snapshot = snapshotFromEffect(instance);
			snapshots.add(snapshot);
			holderMap.put(snapshot, instance.getEffect());
		}
		List<EffectSnapshot> prepared = EffectOrderer.prepare(snapshots, config);
		List<EffectRenderEntry> result = new ArrayList<>(prepared.size());
		for (EffectSnapshot snapshot : prepared) {
			result.add(new EffectRenderEntry(snapshot, holderMap.get(snapshot)));
		}
		return result;
	}

	private static List<ArmorRenderEntry> createPreviewArmorEntries(List<ArmorSlotKind> order) {
		List<ArmorRenderEntry> list = new ArrayList<>(order.size());
		for (ArmorSlotKind kind : order) {
			ItemStack stack = switch (kind) {
				case HELMET -> {
					ItemStack item = new ItemStack(Items.DIAMOND_HELMET);
					item.setDamageValue(91);
					yield item;
				}
				case CHESTPLATE -> {
					ItemStack item = new ItemStack(Items.DIAMOND_CHESTPLATE);
					item.setDamageValue(132);
					yield item;
				}
				case LEGGINGS -> {
					ItemStack item = new ItemStack(Items.IRON_LEGGINGS);
					item.setDamageValue(200);
					yield item;
				}
				case BOOTS -> {
					ItemStack item = new ItemStack(Items.DIAMOND_BOOTS);
					item.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
					yield item;
				}
				case MAIN_HAND -> {
					ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
					item.setDamageValue(390);
					yield item;
				}
				case OFF_HAND -> {
					ItemStack item = new ItemStack(Items.SHIELD);
					item.setDamageValue(84);
					yield item;
				}
			};
			list.add(new ArmorRenderEntry(snapshotFromStack(kind, stack), stack, kind));
		}
		return list;
	}

	private static List<EffectRenderEntry> createPreviewEffectEntries(EffectsConfig config) {
		List<MobEffectInstance> samples = List.of(
				new MobEffectInstance(MobEffects.SPEED, 3080, 1, false, true, true),
				new MobEffectInstance(MobEffects.HASTE, 840, 0, true, true, true),
				new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, true),
				new MobEffectInstance(MobEffects.WEAKNESS, 280, 0, false, true, true)
		);
		return orderFromInstances(samples, config);
	}

	public static ArmorSlotSnapshot snapshotFromStack(ArmorSlotKind kind, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return ArmorSlotSnapshot.empty(kind);
		}
		boolean unbreakable = stack.has(DataComponents.UNBREAKABLE);
		boolean damageable = !unbreakable && stack.isDamageableItem() && stack.getMaxDamage() > 0;
		int maxDamage = damageable ? stack.getMaxDamage() : 0;
		int damageValue = damageable ? stack.getDamageValue() : 0;
		boolean barVisible = stack.isBarVisible();
		float barFraction = barVisible ? Math.max(0.0f, Math.min(1.0f, stack.getBarWidth() / 13.0f)) : 1.0f;
		String name = stack.getHoverName().getString();
		return new ArmorSlotSnapshot(
				kind,
				true,
				name,
				damageable,
				unbreakable,
				maxDamage,
				damageValue,
				stack.getCount(),
				barVisible,
				barFraction
		);
	}

	public static EffectSnapshot snapshotFromEffect(MobEffectInstance instance) {
		Holder<MobEffect> holder = instance.getEffect();
		MobEffect effect = holder.value();
		String id = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("custom:effect");
		String name = effect.getDisplayName().getString();
		EffectCategoryKind category = mapCategory(effect.getCategory());
		return new EffectSnapshot(
				id,
				name,
				category,
				instance.getAmplifier(),
				instance.getDuration(),
				instance.isInfiniteDuration(),
				instance.isAmbient(),
				instance.isVisible(),
				instance.showIcon(),
				effect.getColor()
		);
	}

	private static EffectCategoryKind mapCategory(MobEffectCategory category) {
		if (category == MobEffectCategory.BENEFICIAL) {
			return EffectCategoryKind.BENEFICIAL;
		}
		if (category == MobEffectCategory.HARMFUL) {
			return EffectCategoryKind.HARMFUL;
		}
		return EffectCategoryKind.NEUTRAL;
	}

	private static EquipmentSlot toEquipmentSlot(ArmorSlotKind kind) {
		return switch (kind) {
			case HELMET -> EquipmentSlot.HEAD;
			case CHESTPLATE -> EquipmentSlot.CHEST;
			case LEGGINGS -> EquipmentSlot.LEGS;
			case BOOTS -> EquipmentSlot.FEET;
			case MAIN_HAND -> EquipmentSlot.MAINHAND;
			case OFF_HAND -> EquipmentSlot.OFFHAND;
		};
	}
}
