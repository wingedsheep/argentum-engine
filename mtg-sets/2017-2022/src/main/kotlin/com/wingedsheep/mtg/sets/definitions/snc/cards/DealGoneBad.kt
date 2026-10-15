package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deal Gone Bad
 * {3}{B}
 * Instant
 * Target creature gets -3/-3 until end of turn. Target player mills three cards.
 */
val DealGoneBad = card("Deal Gone Bad") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -3/-3 until end of turn. Target player mills three cards. " +
        "(They put the top three cards of their library into their graveyard.)"

    spell {
        val creature = target(TargetFilter.Creature)
        val player = target(Targets.Player)
        effect = Effects.ModifyStats(-3, -3, creature) then
            Patterns.Library.mill(3, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Mathias Kollros"
        flavorText = "\"Feed the rumor mills with delicious treachery. Bad blood between those two is a good deal for us.\"\n—Kamiz, Obscura Oculus"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3359f13d-9946-49fa-b621-28b1ea714e55.jpg"
    }
}
