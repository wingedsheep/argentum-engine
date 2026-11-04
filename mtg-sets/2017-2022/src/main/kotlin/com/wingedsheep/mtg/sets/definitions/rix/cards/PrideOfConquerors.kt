package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pride of Conquerors
 * {1}{W}
 * Instant
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * Creatures you control get +1/+1 until end of turn. If you have the city's blessing, those
 * creatures get +2/+2 until end of turn instead.
 *
 * Ascend on an instant is a spell ability (CR 702.131a), not the static ability it is on a
 * permanent, so it is spelled out as the first step of resolution: ten or more permanents grant
 * [Effects.GainCitysBlessing]. The pump then reads `Conditions.YouHaveCitysBlessing`, which sees
 * a blessing this very spell just granted. The board-wide pump is the Virtuous Charge shape,
 * [Effects.ForEachInGroup] over the creatures you control aimed at [EffectTarget.IterationEntity].
 */
val PrideOfConquerors = card("Pride of Conquerors") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "Creatures you control get +1/+1 until end of turn. If you have the city's blessing, those " +
        "creatures get +2/+2 until end of turn instead."

    keywords(Keyword.ASCEND)

    spell {
        effect = Effects.If(
            condition = Conditions.ControlPermanentsAtLeast(10),
            then = Effects.GainCitysBlessing()
        ) then Effects.If(
            condition = Conditions.YouHaveCitysBlessing,
            then = Effects.ForEachInGroup(
                Filters.Group.creaturesYouControl,
                Effects.ModifyStats(2, 2, EffectTarget.IterationEntity)
            ),
            otherwise = Effects.ForEachInGroup(
                Filters.Group.creaturesYouControl,
                Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff0a769c-6602-4f6f-a388-f5aa15fb3c90.jpg"
    }
}
