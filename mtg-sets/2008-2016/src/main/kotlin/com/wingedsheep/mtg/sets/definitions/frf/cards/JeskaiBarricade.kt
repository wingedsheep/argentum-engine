package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jeskai Barricade
 * {1}{W}
 * Creature — Wall
 * 0/4
 * Flash (You may cast this spell any time you could cast an instant.)
 * Defender
 * When this creature enters, you may return another target creature you control to its owner's hand.
 *
 * The target is chosen as the trigger goes on the stack; "you may" is asked on resolution, so a
 * declined return leaves the creature where it is.
 */
val JeskaiBarricade = card("Jeskai Barricade") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Wall"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Defender\n" +
        "When this creature enters, you may return another target creature you control to its owner's hand."
    power = 0
    toughness = 4

    keywords(Keyword.FLASH, Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.May(Effects.ReturnToHand(creature))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3aa2d511-6eec-42dd-9ebe-1260c8527856.jpg"
    }
}
