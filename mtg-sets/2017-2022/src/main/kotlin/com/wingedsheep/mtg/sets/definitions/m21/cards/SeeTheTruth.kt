package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * See the Truth — Core Set 2021 #69 (canonical printing)
 * {1}{U} · Sorcery
 *
 * Look at the top three cards of your library. Put one of those cards into your hand and the rest
 * on the bottom of your library in any order. If this spell was cast from anywhere other than your
 * hand, put each of those cards into your hand instead.
 *
 * "Cast from anywhere other than your hand" is `All(WasCast, Not(WasCastFromHand))`, not a bare
 * `Not(WasCastFromHand)`: a copy wasn't cast at all (its cast origin is cleared), so it falls to
 * the Anticipate branch and you only get one card (ruling). The "instead" branch moves the top
 * three straight to hand — it isn't a draw.
 */
val SeeTheTruth = card("See the Truth") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Look at the top three cards of your library. Put one of those cards into your hand " +
        "and the rest on the bottom of your library in any order. If this spell was cast from anywhere " +
        "other than your hand, put each of those cards into your hand instead."

    spell {
        effect = Effects.If(
            condition = Conditions.All(Conditions.WasCast, Conditions.Not(Conditions.WasCastFromHand)),
            then = Effects.Pipeline {
                toHand(gather(CardSource.TopOfLibrary(3)))
            },
            otherwise = Patterns.Library.lookAtTopAndKeep(
                count = 3,
                keepCount = 1,
                keepDestination = CardDestination.ToZone(Zone.HAND),
                restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                restOrder = CardOrder.ControllerChooses
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Ekaterina Burmak"
        flavorText = "\"What is this that is neither future nor past?\""
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91af15cc-fdd5-4d72-a53a-314fa5353527.jpg?1783930719"
        ruling(
            "2020-06-23",
            "See the Truth doesn't give you permission to cast it from a zone other than your hand. " +
                "You'll need to find another way to do so."
        )
        ruling(
            "2020-06-23",
            "If an effect copies See the Truth while it's on the stack, the copy wasn't cast at all, so " +
                "you only get one of the cards it has you look at."
        )
    }
}
