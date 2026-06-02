package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Owl Familiar
 * {1}{U}
 * Creature - Bird
 * 1/1
 * Flying
 * When Owl Familiar enters, draw a card, then discard a card.
 */
val OwlFamiliar = card("Owl Familiar") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = HandPatterns.loot()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Janine Johnston"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9587bcb-0ece-4b36-85dc-76899e403b08.jpg"
    }
}
