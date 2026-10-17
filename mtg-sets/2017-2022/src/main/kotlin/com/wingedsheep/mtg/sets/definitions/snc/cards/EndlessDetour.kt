package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Endless Detour
 * {G}{W}{U}
 * Instant
 *
 * The owner of target spell, nonland permanent, or card in a graveyard puts it on their choice
 * of the top or bottom of their library.
 *
 * One target drawn from a three-zone union (stack, battlefield, graveyard). The owner makes the
 * top/bottom choice on resolution.
 */
val EndlessDetour = card("Endless Detour") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Instant"
    oracleText = "The owner of target spell, nonland permanent, or card in a graveyard puts it on " +
        "their choice of the top or bottom of their library."

    spell {
        val t = target(
            TargetFilter.anyOf(
                TargetFilter.SpellOnStack,
                TargetFilter.NonlandPermanent,
                TargetFilter.CardInGraveyard
            )
        )
        effect = Effects.PutOnTopOrBottomOfLibrary(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Eddie Mendoza"
        flavorText = "After diverting around the eleventh construction barrier, the taxi driver " +
            "began to suspect she was being herded into a trap."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13798c8c-1aa5-4f95-979b-b971e73d715f.jpg?1783923087"
    }
}
