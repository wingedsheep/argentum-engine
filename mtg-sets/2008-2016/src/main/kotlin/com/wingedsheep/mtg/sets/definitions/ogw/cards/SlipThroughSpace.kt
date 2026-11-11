package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Slip Through Space
 * {U}
 * Sorcery
 * Devoid (This card has no color.)
 * Target creature can't be blocked this turn.
 * Draw a card.
 *
 * Teleport's until-end-of-turn [AbilityFlag.CANT_BE_BLOCKED] grant, then a cantrip draw. If the
 * target is illegal on resolution the whole spell fizzles and no card is drawn.
 */
val SlipThroughSpace = card("Slip Through Space") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Target creature can't be blocked this turn.\n" +
        "Draw a card."

    keywords(Keyword.DEVOID)

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Raymond Swanland"
        flavorText = "The Eldrazi twisted in a direction that had no name, and a frontal assault became a panicked rout."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dee6dca-9c7e-4347-bea7-7fecc69e5827.jpg?1783937920"
    }
}
