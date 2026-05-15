package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect

/**
 * Snakeskin Veil
 * {G}
 * Instant
 * Put a +1/+1 counter on target creature you control. It gains hexproof until end of turn. (It can't be the target of spells or abilities your opponents control.)
 */
val SnakeskinVeil = card("Snakeskin Veil") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature you control. It gains hexproof until end of turn. (It can't be the target of spells or abilities your opponents control.)"

    spell {
        val target = target("target creature you control", Targets.CreatureYouControl)
        effect = CompositeEffect(listOf(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, target),
            Effects.GrantKeyword(Keyword.HEXPROOF, target)
        ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Matt Stewart"
        flavorText = "\"Does a serpent roar and chase its prey? No. A serpent waits silently for oblivious prey to draw near.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e692c208-c171-4964-9207-43c2cbc62845.jpg?1631050946"
    }
}
