package com.wingedsheep.mtg.sets.definitions.csp.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming

/**
 * Mishra's Bauble
 * {0}
 * Artifact
 * {T}, Sacrifice this artifact: Look at the top card of target player's library. Draw a card at
 * the beginning of the next turn's upkeep.
 *
 * The look is a lone non-destructive gather of the target player's top card (shown to the
 * activator only, nothing moves — the Callous Deceiver shape). The draw is a step-based delayed
 * trigger on the next turn's upkeep (`timing = NEXT_TURN`, no `fireOnPlayer`, so whoever's turn
 * comes next — Arcane Denial's shape); the ability's controller draws.
 */
val MishrasBauble = card("Mishra's Bauble") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice this artifact: Look at the top card of target player's library. " +
        "Draw a card at the beginning of the next turn's upkeep."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            gather(CardSource.TopOfLibrary(1, player.asPlayer))
        } then Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            timing = DelayedTriggerTiming.NEXT_TURN,
            effect = Effects.DrawCards(1)
        )
        description = "{T}, Sacrifice this artifact: Look at the top card of target player's library. " +
            "Draw a card at the beginning of the next turn's upkeep."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Chippy"
        flavorText = "\"Arcum is a babbling fool! Phyrexian technology is our greatest blessing. " +
            "Take this delightful trinket for instance . . .\"\n—Heidar, Rimewind master"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a720448-017f-4f4a-9501-678245eaed17.jpg?1783943319"
    }
}
