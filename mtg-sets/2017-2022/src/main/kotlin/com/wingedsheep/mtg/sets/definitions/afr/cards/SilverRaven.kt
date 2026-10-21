package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Silver Raven
 * {U}
 * Artifact Creature — Bird
 * 1/1
 * Flying
 * When this creature enters, scry 1.
 */
val SilverRaven = card("Silver Raven") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Bird"
    oracleText = "Flying\nWhen this creature enters, scry 1. (Look at the top card of your library. You may put that card on the bottom.)"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Joe Slucher"
        flavorText = "Wings take flight on whispered wind."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a671ec19-32f4-4fde-8ee0-1224471e79db.jpg?1783926507"
    }
}
