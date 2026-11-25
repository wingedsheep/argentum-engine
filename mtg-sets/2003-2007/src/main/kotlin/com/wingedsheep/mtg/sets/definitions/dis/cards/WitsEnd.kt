package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wit's End
 * {5}{B}{B}
 * Sorcery
 * Target player discards their hand.
 */
val WitsEnd = card("Wit's End") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards their hand."

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Hand.discardHand(player)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Kev Walker"
        flavorText = "\"Some images are so powerful that one glance burns them into your mind forever. Come, let us gaze on nothingness.\"\n—Szadek"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68f8e20c-6d8e-45a1-aabd-176d8df843db.jpg?1783943424"
    }
}
