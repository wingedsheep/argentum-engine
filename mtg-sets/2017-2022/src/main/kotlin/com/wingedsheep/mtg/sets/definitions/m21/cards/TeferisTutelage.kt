package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Teferi's Tutelage
 * {2}{U}
 * Enchantment
 * When this enchantment enters, draw a card, then discard a card.
 * Whenever you draw a card, target opponent mills two cards.
 *
 * An ETB loot ([Patterns.Hand.loot]) plus Queza's per-card draw trigger aimed at a mill.
 */
val TeferisTutelage = card("Teferi's Tutelage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, draw a card, then discard a card.\n" +
        "Whenever you draw a card, target opponent mills two cards. (They put the top two cards of " +
        "their library into their graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.loot()
    }

    triggeredAbility {
        trigger = Triggers.you.draws()
        val opponent = target(Targets.Opponent)
        effect = Patterns.Library.mill(2, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Ilse Gort"
        flavorText = "\"Tarassel's sixth law of temporal recursion is easy to understand as long as you know " +
            "the first seven.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c26450d4-125f-423d-b074-3c959460c242.jpg?1783930718"
        ruling(
            "2020-06-23",
            "You draw a card and discard a card all while the first ability is resolving. Nothing can " +
                "happen between the two, and no player may choose to take actions. This means that the " +
                "second ability is put onto the stack after you've finished resolving the first ability."
        )
    }
}
