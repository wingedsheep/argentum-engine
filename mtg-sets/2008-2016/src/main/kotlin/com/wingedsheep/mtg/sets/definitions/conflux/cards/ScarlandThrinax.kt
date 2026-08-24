package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scarland Thrinax
 * {B}{R}{G}
 * Creature — Lizard
 * 2/2
 * Sacrifice a creature: Put a +1/+1 counter on this creature.
 *
 * "Sacrifice a creature" accepts any creature you control, this one included, so the cost is
 * [Costs.Sacrifice] over [GameObjectFilter.Creature] rather than `SacrificeAnother` (cf.
 * Falkenrath Torturer). The effect is [Effects.AddCounters] on [EffectTarget.Self]; there is no
 * mana in the cost and no tap symbol, so the ability is repeatable as long as creatures last.
 */
val ScarlandThrinax = card("Scarland Thrinax") {
    manaCost = "{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Creature — Lizard"
    power = 2
    toughness = 2
    oracleText = "Sacrifice a creature: Put a +1/+1 counter on this creature."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Daarken"
        flavorText = "\"There is only one way of life in Jund: feed on the weak until you are cut down by something stronger.\"\n—Jorshu of Clan Nel Toth"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2a179b9-e962-49a4-ad92-8cd0291296c1.jpg"
    }
}
