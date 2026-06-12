package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Stargaze
 * {X}{B}{B}
 * Sorcery
 * Look at twice X cards from the top of your library. Put X cards from among them
 * into your hand and the rest into your graveyard. You lose X life.
 */
val Stargaze = card("Stargaze") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Look at twice X cards from the top of your library. Put X cards from among them into your hand and the rest into your graveyard. You lose X life."

    spell {
        effect = Effects.Pipeline {
            // Gather twice X cards from top of library
            val looked = gather(
                CardSource.TopOfLibrary(DynamicAmount.Multiply(DynamicAmount.XValue, 2)),
                name = "looked"
            )
            // Select exactly X to keep
            val (kept, rest) = chooseExactlySplit(
                DynamicAmount.XValue, from = looked,
                selectedLabel = "Put in hand",
                remainderLabel = "Put in graveyard",
                name = "kept",
                remainderName = "rest"
            )
            // Move selected to hand
            move(kept, CardDestination.ToZone(Zone.HAND))
            // Move rest to graveyard
            move(rest, CardDestination.ToZone(Zone.GRAVEYARD))
            // Lose X life
            run(Effects.LoseLife(DynamicAmount.XValue, EffectTarget.Controller))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Serena Malyon"
        flavorText = "\"Some batfolk dedicate their lives to seeing the world beyond them.\"\n—Warion, scholar of the Cosmos"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/777fc599-8de7-44d2-8fdd-9bddf5948a0c.jpg?1721426524"
    }
}
