package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heroic Charge
 * {2}{W}{W}
 * Instant
 * Kicker {1}{R}
 * Creatures you control get +2/+1 until end of turn. If this spell was kicked, those creatures
 * also gain trample until end of turn.
 *
 * One group pass grants both halves, so "those creatures" is exactly the set that got +2/+1.
 */
val HeroicCharge = card("Heroic Charge") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "WR"
    typeLine = "Instant"
    oracleText = "Kicker {1}{R} (You may pay an additional {1}{R} as you cast this spell.)\nCreatures you control get +2/+1 until end of turn. If this spell was kicked, those creatures also gain trample until end of turn."

    keywordAbility(KeywordAbility.kicker("{1}{R}"))

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(2, 1, EffectTarget.IterationEntity) then Effects.If(
                Conditions.WasKicked,
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Zoltan Boros"
        flavorText = "\"For Keld! For Benalia! For Dominaria!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4aab611c-64e0-4b38-97b9-b29a1c6a79b7.jpg?1783921364"
    }
}
