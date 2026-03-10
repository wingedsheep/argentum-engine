package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Clever Impersonator
 * {2}{U}{U}
 * Creature — Shapeshifter
 * 0/0
 * You may have Clever Impersonator enter the battlefield as a copy of any nonland permanent on the battlefield.
 */
val CleverImpersonator = card("Clever Impersonator") {
    manaCost = "{2}{U}{U}"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "You may have Clever Impersonator enter the battlefield as a copy of any nonland permanent on the battlefield."

    replacementEffect(EntersAsCopy(optional = true, copyFilter = GameObjectFilter.NonlandPermanent))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "34"
        artist = "Slawomir Maniak"
        flavorText = "\"Our own selves are the greatest obstacles to enlightenment.\" —Narset, khan of the Jeskai"
        imageUri = "https://cards.scryfall.io//normal/front/c/d/cd8fffd3-81ad-47e3-a27b-d8059f2b506f.jpg?1562793709"
    }
}
