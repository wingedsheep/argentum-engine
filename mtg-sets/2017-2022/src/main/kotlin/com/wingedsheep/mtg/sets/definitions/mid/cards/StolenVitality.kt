package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Stolen Vitality — Innistrad: Midnight Hunt #161
 * {1}{R} · Instant
 *
 * Target creature gets +3/+1 until end of turn. If it's your turn, that creature gains trample until
 * end of turn. Otherwise, it gains first strike until end of turn.
 *
 * The turn check is made on resolution: `Effects.If(Conditions.IsYourTurn, otherwise = …)` picks
 * trample or first strike after the unconditional pump.
 */
val StolenVitality = card("Stolen Vitality") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+1 until end of turn. If it's your turn, that creature gains " +
        "trample until end of turn. Otherwise, it gains first strike until end of turn. (A creature with " +
        "trample can deal excess combat damage to the player or planeswalker it's attacking. A creature " +
        "with first strike deals combat damage before creatures without first strike.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 1, creature) then
            Effects.If(
                condition = Conditions.IsYourTurn,
                then = Effects.GrantKeyword(Keyword.TRAMPLE, creature),
                otherwise = Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Mike Bierek"
        flavorText = "\"There's nothing like a good meal to really lift the spirit.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d50df2dd-8d14-4d92-ab4e-bf9f9f147e91.jpg?1783925590"
    }
}
