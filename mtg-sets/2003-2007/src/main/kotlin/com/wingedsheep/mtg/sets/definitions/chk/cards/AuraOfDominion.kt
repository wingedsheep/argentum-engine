package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aura of Dominion
 * {U}{U}
 * Enchantment — Aura
 * Enchant creature
 * {1}, Tap an untapped creature you control: Untap enchanted creature.
 *
 * The tapped creature is a cost, not a {T} symbol, so any untapped creature you control pays it —
 * including a summoning-sick one, and including the enchanted creature itself.
 */
val AuraOfDominion = card("Aura of Dominion") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n{1}, Tap an untapped creature you control: Untap enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.TapPermanents(1, GameObjectFilter.Creature))
        effect = Effects.Untap(EffectTarget.EnchantedCreature)
        description = "{1}, Tap an untapped creature you control: Untap enchanted creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Randy Gallegos"
        flavorText = "\"Lies and deceit do yield results, but legitimate authority is the ultimate form of control.\"\n—Meloku the Clouded Mirror"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26913c27-5794-42c7-a2a2-af565ce84fd1.jpg?1783944330"
    }
}
