package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nimana Skydancer — Zendikar Rising #117
 * {2}{B} · Creature — Human Rogue · 2/1
 *
 * Flash
 * Flying
 * When this creature enters, target opponent mills two cards.
 */
val NimanaSkydancer = card("Nimana Skydancer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    oracleText = "Flash\nFlying\nWhen this creature enters, target opponent mills two cards. " +
        "(They put the top two cards of their library into their graveyard.)"
    power = 2
    toughness = 1

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Patterns.Library.mill(2, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Bryan Sola"
        flavorText = "The Free City of Nimana is a great place to recruit adventurers with shady skill sets."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e72eaa0f-cbec-45a1-b390-cbbedf5a02f2.jpg?1783929372"
    }
}
