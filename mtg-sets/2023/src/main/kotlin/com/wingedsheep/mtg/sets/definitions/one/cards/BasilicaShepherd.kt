package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Basilica Shepherd
 * {3}{W}{W}
 * Creature — Phyrexian Angel
 * 3/3
 * Flying
 * When this creature enters, create two 1/1 colorless Phyrexian Mite artifact creature tokens with
 * toxic 1 and "This token can't block."
 */
val BasilicaShepherd = card("Basilica Shepherd") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, create two 1/1 colorless Phyrexian Mite artifact creature tokens with toxic 1 and \"This token can't block.\" (Players dealt combat damage by them also get a poison counter.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePhyrexianMite(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Allen Williams"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81ef3cfa-63e6-4450-af65-da7f05d13cf3.jpg?1783918086"
    }
}
