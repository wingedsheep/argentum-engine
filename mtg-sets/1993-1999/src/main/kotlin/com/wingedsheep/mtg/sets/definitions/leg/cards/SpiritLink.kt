package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spirit Link
 * {W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature deals damage, you gain that much life.
 */
val SpiritLink = card("Spirit Link") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature (Target a creature as you cast this. This card enters attached to that creature.)\nWhenever enchanted creature deals damage, you gain that much life."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dealsDamage()
        effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e2d35f8-3cf6-4843-9030-0e9a885d836c.jpg?1783948080"
    }
}
