package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aerial Maneuver
 * {1}{W}
 * Instant
 * Target creature gets +1/+1 and gains flying and first strike until end of turn.
 */
val AerialManeuver = card("Aerial Maneuver") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+1 and gains flying and first strike until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, t) then
            Effects.GrantKeyword(Keyword.FLYING, t) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Scott Chou"
        flavorText = "\"When you put all of yourself into an attack, without fear or hesitation, in that instant you may attain greatness.\"\n—Predak, Gateless freemage"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c63e9c49-3fa4-41ad-9eed-19801df103c6.jpg"
    }
}
