package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Amplifire
 * {2}{R}{R}
 * Creature — Elemental
 * 1/1
 * At the beginning of your upkeep, reveal cards from the top of your library until you reveal a
 * creature card. Until your next turn, this creature's base power becomes twice that card's power
 * and its base toughness becomes twice that card's toughness. Put the revealed cards on the bottom
 * of your library in a random order.
 *
 * Gather-until-match pipeline (Goblin Machinist shape) feeding a layer-7b [Effects.SetBasePowerAndToughness]
 * snapshot that reads the revealed creature card's power/toughness before the cards move. With no
 * creature revealed the match slot is empty and both amounts read 0 — the 0/0 the ruling demands.
 */
val Amplifire = card("Amplifire") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 1
    toughness = 1
    oracleText = "At the beginning of your upkeep, reveal cards from the top of your library until you reveal a creature card. Until your next turn, this creature's base power becomes twice that card's power and its base toughness becomes twice that card's toughness. Put the revealed cards on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val (creature, allRevealed) = gatherUntilMatch(GameObjectFilter.Creature)
            reveal(allRevealed)
            run(
                Effects.SetBasePowerAndToughness(
                    power = DynamicAmounts.powerOf(creature.asTarget) * 2,
                    toughness = DynamicAmounts.toughnessOf(creature.asTarget) * 2,
                    target = EffectTarget.Self,
                    duration = Duration.UntilYourNextTurn
                )
            )
            toLibraryBottom(allRevealed, order = CardOrder.Random)
        }
        description = "At the beginning of your upkeep, reveal cards from the top of your library until you reveal a creature card. Until your next turn, this creature's base power becomes twice that card's power and its base toughness becomes twice that card's toughness. Put the revealed cards on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a58c77f-7c15-44f4-8011-6046482a2d08.jpg?1783933686"
        ruling("2019-01-25", "Amplifire will be a 1/1 creature from the time your untap step begins until the time its triggered ability resolves during your upkeep.")
        ruling("2019-01-25", "Any effects that modify Amplifire's power and/or toughness without setting them to a specific number or value will apply after its base power and toughness are set, regardless of the order in which those effects were created. The same is true of counters that modify its power and toughness.")
        ruling("2019-01-25", "If you reveal your entire library and don't reveal a creature card, Amplifire becomes a 0/0 creature and you put your library back in a random order.")
    }
}
