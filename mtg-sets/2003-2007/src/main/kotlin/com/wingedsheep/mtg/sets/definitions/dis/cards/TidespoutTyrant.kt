package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tidespout Tyrant
 * {5}{U}{U}{U}
 * Creature — Djinn
 * 5/5
 * Flying
 * Whenever you cast a spell, return target permanent to its owner's hand.
 */
val TidespoutTyrant = card("Tidespout Tyrant") {
    manaCost = "{5}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn"
    oracleText = "Flying\nWhenever you cast a spell, return target permanent to its owner's hand."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts()
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.ReturnToHand(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Dany Orizio"
        flavorText = "He unmakes the world in his own image."
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44865244-2b9f-4734-a4da-49613b23ee4d.jpg?1783943434"
    }
}
