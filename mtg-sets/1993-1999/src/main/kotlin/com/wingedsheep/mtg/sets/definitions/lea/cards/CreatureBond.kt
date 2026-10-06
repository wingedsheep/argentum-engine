package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Creature Bond
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * When enchanted creature dies, this Aura deals damage equal to that creature's toughness
 * to the creature's controller.
 *
 * "That creature's toughness" and "the creature's controller" are the dying creature's
 * last-known values, read off the zone-change trigger context by
 * [DynamicAmounts.triggeringToughness] and [EffectTarget.ControllerOfTriggeringEntity].
 * The Aura itself has usually gone to the graveyard by resolution; it still deals the damage
 * as it last existed.
 */
val CreatureBond = card("Creature Bond") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When enchanted creature dies, this Aura deals damage equal to that creature's toughness " +
        "to the creature's controller."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.DealDamage(DynamicAmounts.triggeringToughness(), EffectTarget.ControllerOfTriggeringEntity)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee4bd7d1-77e5-46e5-a594-c24469e88c4c.jpg?1783948707"
    }
}
