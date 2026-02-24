package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Utter End
 * {2}{W}{B}
 * Instant
 * Exile target nonland permanent.
 */
val UtterEnd = card("Utter End") {
    manaCost = "{2}{W}{B}"
    typeLine = "Instant"
    oracleText = "Exile target nonland permanent."

    spell {
        val t = target("target", Targets.NonlandPermanent)
        effect = MoveToZoneEffect(t, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Mark Winters"
        flavorText = "\"I came seeking a challenge. All I found was you.\" —Zurgo, khan of the Mardu"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39643107-8873-42f6-9b4d-b546a0a976ba.jpg?1562784988"
    }
}
