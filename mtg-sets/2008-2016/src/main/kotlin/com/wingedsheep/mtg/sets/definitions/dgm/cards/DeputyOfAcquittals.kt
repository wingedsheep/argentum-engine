package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deputy of Acquittals
 * {W}{U}
 * Creature — Human Wizard
 * 2/2
 * Flash (You may cast this spell any time you could cast an instant.)
 * When this creature enters, you may return another target creature you control to its owner's hand.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val DeputyOfAcquittals = card("Deputy of Acquittals") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "When this creature enters, you may return another target creature you control to its owner's hand."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.May(Effects.ReturnToHand(creature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "James Ryman"
        flavorText = "\"Everyone deserves a day in court.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b555888-21b1-4c45-966d-d98f32460d4e.jpg"
    }
}
