package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Coralhelm Chronicler — Zendikar Rising #54
 * {2}{U} · Creature — Merfolk Wizard · 2/2
 *
 * Whenever you cast a kicked spell, draw a card, then discard a card.
 * When this creature enters, look at the top five cards of your library. You may reveal a card with
 * a kicker ability from among them and put it into your hand. Put the rest on the bottom of your
 * library in a random order.
 */
val CoralhelmChronicler = card("Coralhelm Chronicler") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "Whenever you cast a kicked spell, draw a card, then discard a card.\n" +
        "When this creature enters, look at the top five cards of your library. You may reveal a card with a " +
        "kicker ability from among them and put it into your hand. Put the rest on the bottom of your library " +
        "in a random order."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.WasKicked))
        effect = Patterns.Hand.loot(draw = 1, discard = 1)
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Any.withKicker(),
            prompt = "You may reveal a card with a kicker ability from among them and put it into your hand",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50b54524-8345-4b47-913f-d883a1cbe3a1.jpg?1783929399"
    }
}
