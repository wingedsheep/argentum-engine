package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Thopter Fabricator
 * {2}{U}
 * Artifact — Vehicle
 * 4/4
 * Flying
 * Whenever you draw your second card each turn, create a 1/1 colorless Thopter artifact creature
 * token with flying.
 * Crew 2
 *
 * "Your second card each turn" is [Triggers.NthCardDrawn] — it reads the per-player draw counter,
 * so it fires exactly once per turn even when a single multi-card draw crosses the threshold, and
 * cards put into hand without the word "draw" (CR 121.5) don't advance it.
 */
val ThopterFabricator = card("Thopter Fabricator") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    oracleText = "Flying\n" +
        "Whenever you draw your second card each turn, create a 1/1 colorless Thopter artifact " +
        "creature token with flying.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 4
    toughness = 4
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/d/3/d38fc294-ad86-441e-96fe-4ca286a11218.jpg?1783907677"
        )
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Racrufi"
        flavorText = "Take flight on the wings of true innovation."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8924b785-b140-4212-a1eb-a10340e09fea.jpg?1783907901"
    }
}
