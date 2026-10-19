package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Mirror Universe
 * {6}
 * Artifact
 * {T}, Sacrifice this artifact: Exchange life totals with target opponent.
 * Activate only during your upkeep.
 */
val MirrorUniverse = card("Mirror Universe") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice this artifact: Exchange life totals with target opponent. " +
        "Activate only during your upkeep."

    activatedAbility {
        val opponent = target(Targets.Opponent)
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.ExchangeLifeTotals(opponent)
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP)
            )
        )
        description = "Exchange life totals with target opponent"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "287"
        artist = "Phil Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8f05d5e-bb7d-4554-b880-f0c6b4688357.jpg?1783948026"
        ruling(
            "2011-01-01",
            "If an effect says that a player can't lose life, that player can't exchange life totals " +
                "with a player who has a lower life total; in that case, the exchange won't happen."
        )
        ruling(
            "2007-09-16",
            "When the life totals are exchanged, each player gains or loses the amount of life necessary " +
                "to equal the other player's previous life total. Replacement effects may modify these gains " +
                "and losses, and triggered abilities may trigger on them."
        )
    }
}
