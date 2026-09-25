package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Soratami Mirror-Mage
 * {3}{U}
 * Creature — Moonfolk Wizard
 * 2/1
 * Flying
 * {3}, Return three lands you control to their owner's hand: Return target creature to its
 * owner's hand.
 */
val SoratamiMirrorMage = card("Soratami Mirror-Mage") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Moonfolk Wizard"
    oracleText = "Flying\n{3}, Return three lands you control to their owner's hand: Return " +
        "target creature to its owner's hand."
    power = 2
    toughness = 1

    keywords(Keyword.FLYING)

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.ReturnToHand(Filters.Land, count = 3))
        effect = Effects.ReturnToHand(creature)
        description = "{3}, Return three lands you control to their owner's hand: Return target " +
            "creature to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Ron Spears"
        flavorText = "\"The clouds obey my whims, and you'll obey theirs.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/1/215209b1-1a5d-48f4-9eca-19a9848b2fed.jpg?1783944321"
    }
}
