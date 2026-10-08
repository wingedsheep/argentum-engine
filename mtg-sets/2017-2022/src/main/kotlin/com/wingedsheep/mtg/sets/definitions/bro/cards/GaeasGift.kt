package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gaea's Gift
 * {1}{G}
 * Instant
 * Put a +1/+1 counter on target creature you control. It gains reach, trample, hexproof, and
 * indestructible until end of turn. (It can't be the target of spells or abilities your opponents
 * control. Damage and effects that say "destroy" don't destroy it.)
 */
val GaeasGift = card("Gaea's Gift") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature you control. It gains reach, trample, hexproof, and indestructible until end of turn. (It can't be the target of spells or abilities your opponents control. Damage and effects that say \"destroy\" don't destroy it.)"

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.REACH, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
            Effects.GrantKeyword(Keyword.HEXPROOF, creature) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Olivier Bernard"
        flavorText = "When it came to fighting an army of trees, even Mishra's best generals were stumped."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5503186a-46fe-4956-8ae3-5ab3343f8a93.jpg"
    }
}
