package com.wingedsheep.mtg.sets.definitions.foundations.cards

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
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Dan Murayama Scott"
        flavorText = "Most travelers panic when they stumble upon the skin of a giant rattlewurm. Landry saw it as a gift."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cc4c21d-9bdc-4490-9203-17f51db0ddd1.jpg?1730489471"
    }
}
