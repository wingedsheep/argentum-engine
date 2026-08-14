package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.model.Rarity

/**
 * Spider-Man, Web-Slinger — Marvel's Spider-Man #16
 * {2}{W} · Legendary Creature — Spider Human Hero · 3/3
 *
 * Web-slinging {W} (You may cast this spell for {W} if you also return a tapped creature you
 * control to its owner's hand.)
 *
 * The plainest web-slinger: a vanilla 3/3 whose only ability is the [webSlinging] alternative cost
 * (CR 702.188). All behavior lives in the engine's web-slinging pipeline; the card just declares the
 * keyword.
 */
val SpiderManWebSlinger = card("Spider-Man, Web-Slinger") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 3
    toughness = 3
    oracleText = "Web-slinging {W} (You may cast this spell for {W} if you also return a tapped " +
        "creature you control to its owner's hand.)"

    webSlinging("{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Ryan Pancoast"
        flavorText = "\"Nothing like a little crimefighting before class.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/9/897418bc-df8c-4c97-b6bf-7c9133a8a577.jpg?1783905360"
    }
}
