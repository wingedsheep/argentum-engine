package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Give No Ground
 * {3}{W}
 * Instant
 * Target creature gets +2/+6 until end of turn and can block any number of creatures this turn.
 */
val GiveNoGround = card("Give No Ground") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+6 until end of turn and can block any number of creatures this turn."
    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 6, creature) then
            Effects.GrantStaticAbility(CanBlockAnyNumber(), creature, Duration.EndOfTurn)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Tyler Jacobson"
        flavorText = "\"Get behind me!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1cb3347a-9b32-46a4-a621-4ae5244ba2f5.jpg?1783937516"
    }
}
