package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * War Flare
 * {2}{R}{W}
 * Instant
 * Creatures you control get +2/+1 until end of turn. Untap those creatures.
 *
 * One pass over the group, so "those creatures" are exactly the ones that got the pump.
 */
val WarFlare = card("War Flare") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "WR"
    typeLine = "Instant"
    oracleText = "Creatures you control get +2/+1 until end of turn. Untap those creatures."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(2, 1, EffectTarget.IterationEntity) then
                Effects.Untap(EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Karl Kopinski"
        flavorText = "\"No sun or fire can warm my blood quite like a war flare.\"\n—Urut Barzeel, Mardu hordechief"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/243f33d8-ec20-4376-99c2-a12098773dee.jpg?1783938677"
    }
}
