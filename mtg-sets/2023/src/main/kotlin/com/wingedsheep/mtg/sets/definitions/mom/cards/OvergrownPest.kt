package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Overgrown Pest
 * {2}{G}
 * Creature — Pest
 * 2/2
 * When this creature enters, look at the top five cards of your library. You may reveal a land or
 * double-faced card from among them and put that card into your hand. Put the rest on the bottom of
 * your library in a random order.
 */
val OvergrownPest = card("Overgrown Pest") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Pest"
    oracleText = "When this creature enters, look at the top five cards of your library. You may reveal a land or double-faced card from among them and put that card into your hand. Put the rest on the bottom of your library in a random order."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Land or Filters.DoubleFaced,
            prompt = "You may reveal a land or double-faced card from among them and put it into your hand"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Eelis Kyttanen"
        flavorText = "If only the biofluctuations students could have seen what became of their prize specimen after its escape."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1e04eed-9a6c-491b-8f1f-1c76ac40452d.jpg?1783916966"
    }
}
