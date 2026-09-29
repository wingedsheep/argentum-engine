package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Zhalfirin Shapecraft
 * {1}{U}
 * Instant
 * Target creature has base power and toughness 4/3 until end of turn.
 * Draw a card.
 */
val ZhalfirinShapecraft = card("Zhalfirin Shapecraft") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature has base power and toughness 4/3 until end of turn.\nDraw a card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.SetBasePowerAndToughness(4, 3, creature, Duration.EndOfTurn) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Aldo Domínguez"
        flavorText = "Patient and observant, the people of Zhalfir lurked beyond the veil of time " +
            "until their moment came to finally strike at Phyrexia."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e446a380-0316-46f7-8ac9-22bce773b35f.jpg?1783917019"
    }
}
