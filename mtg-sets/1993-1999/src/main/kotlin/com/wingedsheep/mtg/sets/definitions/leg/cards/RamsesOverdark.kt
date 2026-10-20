package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ramses Overdark — Legends #252
 * {2}{U}{U}{B}{B} · Legendary Creature — Human Assassin · 4/3
 *
 * {T}: Destroy target enchanted creature.
 *
 * "Enchanted" is [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsEnchanted] — any Aura
 * attached, whoever controls it — re-checked on resolution, so a creature whose Aura leaves in
 * response is no longer a legal target.
 */
val RamsesOverdark = card("Ramses Overdark") {
    manaCost = "{2}{U}{U}{B}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Assassin"
    power = 4
    toughness = 3
    oracleText = "{T}: Destroy target enchanted creature."

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature.enchanted())
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Richard Kane Ferguson"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f079c74e-a39a-40f9-9c7e-9319c0c189c6.jpg?1783948033"
    }
}
