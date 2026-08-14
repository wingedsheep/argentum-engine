package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Racers' Scoreboard — Aetherdrift #239
 * {4} · Artifact
 *
 * Start your engines!
 * When this artifact enters, draw two cards, then discard a card.
 * Max speed — Spells you cast cost {1} less to cast.
 *
 * The cost reduction is a [ModifySpellCost] over every spell its controller casts. Declaring it
 * inside [maxSpeed] folds [com.wingedsheep.sdk.dsl.Conditions.YouHaveMaxSpeed] into the modifier's
 * own `gating` slot rather than wrapping it in a conditional static ability — cost calculation reads
 * the raw static list and never unwraps a conditional, so a wrapper would make the reduction
 * silently never apply.
 *
 * "Draw two cards, then discard a card" is sequential, not a single rummage: the discard sees the two
 * freshly drawn cards, so a hand that was empty still has three cards to choose from.
 */
val RacersScoreboard = card("Racers' Scoreboard") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Start your engines!\n" +
        "When this artifact enters, draw two cards, then discard a card.\n" +
        "Max speed — Spells you cast cost {1} less to cast."

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.DrawCards(2),
            Effects.Discard(1)
        )
    }

    maxSpeed {
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGeneric(1)
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Konstantin Porubov"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50bae2ba-a6a0-4a6a-96e9-0e0372e55108.jpg?1783907847"
    }
}
