package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Failed Fording
 * {1}{U}
 * Instant
 *
 * Return target nonland permanent to its owner's hand. If you control a Desert, surveil 1.
 *
 * The "If you control a Desert, surveil 1" clause is a one-shot resolution-time state test, not
 * an intervening-if trigger — modeled as a [ConditionalEffect] (lowers to a `Gate.WhenCondition`)
 * chained after the bounce. The Desert check uses [Conditions.YouControl] over Lands with the
 * Desert subtype.
 */
val FailedFording = card("Failed Fording") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target nonland permanent to its owner's hand. If you control a Desert, " +
        "surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    spell {
        val permanent = target("target nonland permanent", Targets.NonlandPermanent)
        effect = Effects.ReturnToHand(permanent)
            .then(
                ConditionalEffect(
                    condition = Conditions.YouControl(
                        GameObjectFilter.Land.withSubtype(Subtype.DESERT)
                    ),
                    effect = Patterns.Library.surveil(1)
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "José Parodi"
        flavorText = "\"We should have taken the blasted ferry!\"\n—Hope Hagan, prospector"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62bbe11b-e959-4080-98ac-09bd57519c00.jpg?1712355418"
    }
}
