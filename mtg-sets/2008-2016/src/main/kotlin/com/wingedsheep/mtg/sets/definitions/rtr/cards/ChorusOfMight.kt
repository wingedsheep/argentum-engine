package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chorus of Might — Return to Ravnica #119
 * {3}{G} · Instant
 *
 * Until end of turn, target creature gets +1/+1 for each creature you control and gains trample.
 *
 * The King Harald's Revenge shape: the count is taken as the spell resolves and locked in.
 */
val ChorusOfMight = card("Chorus of Might") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +1/+1 for each creature you control and gains trample."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            DynamicAmounts.creaturesYouControl(),
            DynamicAmounts.creaturesYouControl(),
            creature,
        ) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Christopher Moeller"
        flavorText = "\"In each of us is the strength of all of us.\"\n—Trostani"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/214dc9c1-154d-4c35-845d-dd2928f1e142.jpg?1783940349"
        ruling(
            "2012-10-01",
            "The number of creatures you control is counted only when Chorus of Might resolves.",
        )
    }
}
