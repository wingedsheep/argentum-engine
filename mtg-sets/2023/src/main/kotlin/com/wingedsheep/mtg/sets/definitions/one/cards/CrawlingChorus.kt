package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Crawling Chorus
 * {W}
 * Creature — Phyrexian Horror
 * 1/1
 * Toxic 1
 * When this creature dies, create a 1/1 colorless Phyrexian Mite artifact creature token with
 * toxic 1 and "This token can't block."
 */
val CrawlingChorus = card("Crawling Chorus") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Horror"
    power = 1
    toughness = 1
    oracleText = "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "When this creature dies, create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and \"This token can't block.\""

    keywordAbility(KeywordAbility.toxic(1))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreatePhyrexianMite()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Michael Walsh"
        flavorText = "\"We need not be swift. We are inexorable.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aace4c44-7250-414b-aac4-df042a1e2e1d.jpg?1783918086"
    }
}
