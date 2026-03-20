package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sinister Monolith
 * {3}{B}
 * Artifact
 * At the beginning of combat on your turn, each opponent loses 1 life and you gain 1 life.
 * {T}, Pay 2 life, Sacrifice this artifact: Draw two cards. Activate only as a sorcery.
 */
val SinisterMonolith = card("Sinister Monolith") {
    manaCost = "{3}{B}"
    typeLine = "Artifact"
    oracleText = "At the beginning of combat on your turn, each opponent loses 1 life and you gain 1 life.\n{T}, Pay 2 life, Sacrifice this artifact: Draw two cards. Activate only as a sorcery."

    // At the beginning of combat on your turn, each opponent loses 1 life and you gain 1 life.
    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = Effects.Composite(
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)),
            Effects.GainLife(1)
        )
    }

    // {T}, Pay 2 life, Sacrifice this artifact: Draw two cards. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(2), Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Adam Paquette"
        flavorText = "\"Blood and brine make a strong mortar.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a15e06c-2608-4e7a-a16c-d35417669d86.jpg?1721426512"
        ruling("2024-07-26", "The beginning of combat step happens every turn, even if there are no creatures on the battlefield that could attack.")
    }
}
