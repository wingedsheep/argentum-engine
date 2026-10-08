package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Distorting Wake
 * {X}{U}{U}{U}
 * Sorcery
 * Return X target nonland permanents to their owners' hands.
 *
 * "X target nonland permanents" is exactly X (`targets(…, exactly = X)`, the Builder's Bane
 * pattern): the targeting overlay requires the X chosen at cast time, X = 0 is a legal cast with no
 * targets, and an X larger than the number of nonland permanents isn't offered. The chosen targets
 * are gathered and bounced via the standard gather → move-to-hand pipeline.
 */
val DistortingWake = card("Distorting Wake") {
    manaCost = "{X}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return X target nonland permanents to their owners' hands."

    spell {
        targets(TargetFilter.NonlandPermanent, exactly = DynamicAmounts.xValue())
        effect = Effects.Pipeline {
            val distortingWakeTargets = gather(CardSource.ChosenTargets)
            toHand(distortingWakeTargets)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf48eec9-96be-4f53-9d9a-c6f02d44c995.jpg?1562936657"
    }
}
