package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Repeal
 * {X}{U}
 * Instant
 * Return target nonland permanent with mana value X to its owner's hand.
 * Draw a card.
 *
 * X is chosen as the spell is cast (paid as part of its {X}{U} mana cost). The targeted
 * nonland permanent must have mana value exactly X (`manaValueEqualsX()`, the
 * same chosen-number target restriction as Spell Blast / Blue Sun's Twilight).
 */
val Repeal = card("Repeal") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target nonland permanent with mana value X to its owner's hand.\nDraw a card."

    spell {
        val t = target(TargetFilter.NonlandPermanent.manaValueEqualsX())
        effect = Effects.ReturnToHand(t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e7dd929-4bba-46a6-86c9-b8ed853eb721.jpg?1593272056"
    }
}
