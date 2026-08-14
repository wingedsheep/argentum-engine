package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Rangers' Aetherhive — Aetherdrift #217
 * {1}{G}{U} · Artifact — Vehicle · 3/5
 */
val RangersAetherhive = card("Rangers' Aetherhive") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 5
    oracleText = "Vigilance\nWhenever you activate an exhaust ability, create a 1/1 colorless " +
        "Thopter artifact creature token with flying.\nCrew 1"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.YouActivateExhaustAbility
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/d/3/d38fc294-ad86-441e-96fe-4ca286a11218.jpg?1783907677",
        )
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Josiah \"Jo\" Cameron"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b238d2c-d10f-496d-aa34-5a1536e056b5.jpg?1783907854"
    }
}
