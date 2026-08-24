package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spore Flower
 * {G}{G}
 * Creature — Fungus
 * 0/1
 * At the beginning of your upkeep, put a spore counter on this creature.
 * Remove three spore counters from this creature: Prevent all combat damage that would be dealt
 * this turn.
 *
 * The prevention is board-wide and undirected — a Fog, not a shield on one creature.
 */
val SporeFlower = card("Spore Flower") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus"
    oracleText = "At the beginning of your upkeep, put a spore counter on this creature.\n" +
        "Remove three spore counters from this creature: Prevent all combat damage that would be dealt this turn."
    power = 0
    toughness = 1

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.AddCounters(Counters.SPORE, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, put a spore counter on this creature."
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(Counters.SPORE, 3)
        effect = Effects.PreventAllCombatDamage()
        description = "Remove three spore counters from this creature: Prevent all combat damage that would be dealt this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Margaret Organ-Kean"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9681dc0-d0fc-4d5b-a23c-63ec1cc8343d.jpg?1783947885"
    }
}
