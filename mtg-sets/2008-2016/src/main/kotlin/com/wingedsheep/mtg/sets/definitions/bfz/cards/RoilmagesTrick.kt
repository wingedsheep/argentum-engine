package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Roilmage's Trick
 * {3}{U}
 * Instant
 * Converge — Creatures your opponents control get -X/-0 until end of turn, where X is the number of
 * colors of mana spent to cast this spell.
 * Draw a card.
 *
 * Converge: X = [DynamicAmounts.colorsOfManaSpent], read while the spell resolves. The group is
 * snapshotted at resolution and each member's -X/-0 is evaluated then and locked in for the turn
 * (Blinding Spray's shape with a dynamic amount), so creatures that arrive later are unaffected.
 * A copy (X = 0) or an opponent with no creatures still draws the card.
 */
val RoilmagesTrick = card("Roilmage's Trick") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Converge — Creatures your opponents control get -X/-0 until end of turn, where X is " +
        "the number of colors of mana spent to cast this spell.\nDraw a card."

    spell {
        effect = Patterns.Group.modifyStatsForAll(
            power = -DynamicAmounts.colorsOfManaSpent(),
            toughness = DynamicAmounts.fixed(0),
            filter = GroupFilter.AllCreaturesOpponentsControl
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Johann Bodin"
        flavorText = "Weather on Zendikar is unpredictable—unless you're the one telling it what to do."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2194713-c08c-4fba-8e31-bb67c9932173.jpg?1783938207"
    }
}
