package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect

/**
 * Biosynthic Burst
 * {1}{G}
 * Instant
 * Put a +1/+1 counter on target creature you control. It gains reach, trample, and indestructible until end of turn. Untap it. (Damage and effects that say "destroy" don't destroy it.)
 */
val BiosynthicBurst = card("Biosynthic Burst") {
    manaCost = "{1}{G}"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature you control. It gains reach, trample, and indestructible until end of turn. Untap it. (Damage and effects that say \"destroy\" don't destroy it.)"

    spell {
        val target = target("target creature you control", Targets.CreatureYouControl)
        effect = CompositeEffect(listOf(
            // Put a +1/+1 counter on target creature
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, target),
            // It gains reach, trample, and indestructible until end of turn
            Effects.GrantKeyword(Keyword.REACH, target, duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn),
            Effects.GrantKeyword(Keyword.TRAMPLE, target, duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, target, duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn),
            // Untap it
            Effects.Untap(target)
        ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Loïc Canavaggia"
        flavorText = "Maybe in your next evolution.\n—Eumidian insult"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d73a4a8-5d52-4c12-a96a-45cd202bcc62.jpg?1752947258"
    }
}
