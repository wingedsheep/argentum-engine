package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shields Up!
 * {1}{W}
 * Instant
 * Target artifact or creature you control gains hexproof and indestructible until end of turn. If
 * it's a creature, put a +1/+1 counter on it.
 *
 * The creature check happens on resolution against the target's projected types, so an artifact
 * animated by then gets the counter and a creature that stopped being one doesn't.
 */
val ShieldsUp = card("Shields Up!") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target artifact or creature you control gains hexproof and indestructible until end of turn. " +
        "If it's a creature, put a +1/+1 counter on it. (It can't be the target of spells or abilities your " +
        "opponents control. Damage and effects that say \"destroy\" don't destroy it.)"

    spell {
        val t = target(TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl()))
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, t) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature, t),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Raymond Swanland"
        flavorText = "Deflector shields use intense graviton fields to create a \"solid\" surface from nothing at all."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b60c9af-8ea9-491d-a4a4-226a2143e7f3.jpg?1785981369"
    }
}
