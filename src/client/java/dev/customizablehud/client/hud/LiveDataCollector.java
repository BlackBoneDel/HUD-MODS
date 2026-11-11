package dev.customizablehud.client.hud;

import dev.customizablehud.client.i18n.ModI18n;
import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.ModLanguage;
import dev.customizablehud.format.ArmorSlotSnapshot;
import dev.customizablehud.format.EffectCategoryKind;
import dev.customizablehud.format.EffectOrderer;
import dev.customizablehud.format.EffectSnapshot;
import dev.customizablehud.format.ModTranslations;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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

	public static List<ArmorRenderEntry> collectArmorEntries(
			Minecraft client,
			ArmorConfig config,
			ModLanguage language,
			boolean previewFallback
	) {
		List<ArmorSlotKind> order = config.activeSlotOrder();
		Player player = client != null ? client.player : null;
		if (player != null) {
			List<ArmorRenderEntry> liveEntries = new ArrayList<>(order.size());
			boolean anyPresent = false;
			for (ArmorSlotKind kind : order) {
				ItemStack stack = player.getItemBySlot(toEquipmentSlot(kind));
				ArmorSlotSnapshot snapshot = snapshotFromStack(kind, stack, language);
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
		return createPreviewArmorEntries(order, language);
	}

	public static List<EffectRenderEntry> collectEffectEntries(
			Minecraft client,
			EffectsConfig config,
			ModLanguage language,
			boolean previewFallback
	) {
		return collectEffectEntries(client, config, language, previewFallback, null);
	}

	public static List<EffectRenderEntry> collectEffectEntries(
			Minecraft client,
			EffectsConfig config,
			ModLanguage language,
			boolean previewFallback,
			String highlightEffectId
	) {
		Player player = client != null ? client.player : null;
		if (player != null) {
			Collection<MobEffectInstance> active = player.getActiveEffects();
			if (!active.isEmpty()) {
				List<EffectRenderEntry> ordered = orderFromInstances(active, config, language);
				if (!previewFallback) {
					return ordered;
				}
				if (highlightEffectId != null) {
					return ensureHighlightedPreviewEntry(ordered, config, language, highlightEffectId);
				}
				if (!ordered.isEmpty()) {
					return ordered;
				}
			} else if (!previewFallback) {
				return List.of();
			}
		}

		if (!previewFallback) {
			return List.of();
		}
		List<EffectRenderEntry> samples = createPreviewEffectEntries(config, language);
		if (highlightEffectId != null) {
			return ensureHighlightedPreviewEntry(samples, config, language, highlightEffectId);
		}
		return samples;
	}

	private static List<EffectRenderEntry> ensureHighlightedPreviewEntry(
			List<EffectRenderEntry> existing,
			EffectsConfig config,
			ModLanguage language,
			String highlightEffectId
	) {
		for (EffectRenderEntry entry : existing) {
			if (highlightEffectId.equals(entry.snapshot().id())) {
				return existing;
			}
		}
		EffectRenderEntry highlighted = createSinglePreviewEntryForId(highlightEffectId, language);
		List<EffectRenderEntry> combined = new ArrayList<>(existing.size() + 1);
		combined.add(highlighted);
		combined.addAll(existing);
		return combined;
	}

	private static EffectRenderEntry createSinglePreviewEntryForId(String effectId, ModLanguage language) {
		Identifier parsedId = Identifier.tryParse(effectId);
		if (parsedId != null) {
			Optional<Holder.Reference<MobEffect>> holderOpt = BuiltInRegistries.MOB_EFFECT.get(parsedId);
			if (holderOpt.isPresent()) {
				MobEffectInstance inst = new MobEffectInstance(holderOpt.get(), 900, 1, false, true, true);
				return new EffectRenderEntry(snapshotFromEffect(inst, language), holderOpt.get());
			}
		}
		EffectSnapshot fallback = new EffectSnapshot(
				effectId,
				ModTranslations.humanizeIdentifier(effectId),
				EffectCategoryKind.NEUTRAL,
				0,
				900,
				false,
				false,
				true,
				true,
				0xE879F9
		);
		return new EffectRenderEntry(fallback, null);
	}

	private static List<EffectRenderEntry> orderFromInstances(
			Collection<MobEffectInstance> instances,
			EffectsConfig config,
			ModLanguage language
	) {
		List<EffectSnapshot> snapshots = new ArrayList<>(instances.size());
		Map<EffectSnapshot, Holder<MobEffect>> holderMap = new IdentityHashMap<>();
		for (MobEffectInstance instance : instances) {
			if (instance == null || instance.getEffect() == null) {
				continue;
			}
			EffectSnapshot snapshot = snapshotFromEffect(instance, language);
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

	private static List<ArmorRenderEntry> createPreviewArmorEntries(List<ArmorSlotKind> order, ModLanguage language) {
		List<ArmorRenderEntry> list = new ArrayList<>(order.size());
		for (ArmorSlotKind kind : order) {
			ItemStack stack = switch (kind) {
				case HELMET -> {
					// Healthy (>60%)
					ItemStack item = new ItemStack(Items.DIAMOND_HELMET);
					item.setDamageValue(91);
					yield item;
				}
				case CHESTPLATE -> {
					// Warning (21-60%)
					ItemStack item = new ItemStack(Items.DIAMOND_CHESTPLATE);
					item.setDamageValue(290);
					yield item;
				}
				case LEGGINGS -> {
					// Critical (<=20%)
					ItemStack item = new ItemStack(Items.IRON_LEGGINGS);
					item.setDamageValue(195);
					yield item;
				}
				case BOOTS -> {
					// Unbreakable
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
			list.add(new ArmorRenderEntry(snapshotFromStack(kind, stack, language), stack, kind));
		}
		return list;
	}

	private static List<EffectRenderEntry> createPreviewEffectEntries(EffectsConfig config, ModLanguage language) {
		List<MobEffectInstance> samples = List.of(
				new MobEffectInstance(MobEffects.REGENERATION, 900, 1, false, true, true),
				new MobEffectInstance(MobEffects.SPEED, 3080, 1, false, true, true),
				new MobEffectInstance(MobEffects.HASTE, 840, 0, true, true, true),
				new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, true),
				new MobEffectInstance(MobEffects.WEAKNESS, 280, 0, false, true, true)
		);
		return orderFromInstances(samples, config, language);
	}

	public static ArmorSlotSnapshot snapshotFromStack(ArmorSlotKind kind, ItemStack stack, ModLanguage language) {
		if (stack == null || stack.isEmpty()) {
			return ArmorSlotSnapshot.empty(kind);
		}
		boolean unbreakable = stack.has(DataComponents.UNBREAKABLE);
		boolean damageable = !unbreakable && stack.isDamageableItem() && stack.getMaxDamage() > 0;
		int maxDamage = damageable ? stack.getMaxDamage() : 0;
		int damageValue = damageable ? stack.getDamageValue() : 0;
		boolean barVisible = damageable && stack.isBarVisible();
		float barFraction = damageable ? Math.max(0.0f, Math.min(1.0f, (maxDamage - Math.max(0, Math.min(maxDamage, damageValue))) / (float) maxDamage)) : 0.0f;

		String name;
		if (stack.has(DataComponents.CUSTOM_NAME)) {
			name = stack.getHoverName().getString();
		} else {
			String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			name = ModI18n.translateExternal(language, stack.getItem().getDescriptionId(), stack.getHoverName(), itemId);
		}

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

	public static EffectSnapshot snapshotFromEffect(MobEffectInstance instance, ModLanguage language) {
		Holder<MobEffect> holder = instance.getEffect();
		MobEffect effect = holder.value();
		String id = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("custom:effect");
		String name = ModI18n.translateExternal(language, effect.getDescriptionId(), effect.getDisplayName(), id);
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
