package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Golden Demise
 * {1}{B}{B}
 * Sorcery
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * All creatures get -2/-2 until end of turn. If you have the city's blessing, instead only creatures
 * your opponents control get -2/-2 until end of turn.
 *
 * Ascend on a sorcery is a spell ability, so it is spelled out as the first step of resolution
 * (the Pride of Conquerors shape): ten or more permanents grant [Effects.GainCitysBlessing], and the
 * shrink then reads `Conditions.YouHaveCitysBlessing`, which sees a blessing this spell just granted.
 * Each branch is [Effects.ForEachInGroup] over the creatures on the battlefield as it resolves, so
 * creatures entering (or changing control) later in the turn are unaffected, per the rulings.
 */
val GoldenDemise = card("Golden Demise") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "All creatures get -2/-2 until end of turn. If you have the city's blessing, instead only creatures " +
        "your opponents control get -2/-2 until end of turn."

    keywords(Keyword.ASCEND)

    spell {
        effect = Effects.If(
            condition = Conditions.ControlPermanentsAtLeast(10),
            then = Effects.GainCitysBlessing()
        ) then Effects.If(
            condition = Conditions.YouHaveCitysBlessing,
            then = Effects.ForEachInGroup(
                Filters.Group.creaturesOpponentsControl,
                Effects.ModifyStats(-2, -2, EffectTarget.IterationEntity)
            ),
            otherwise = Effects.ForEachInGroup(
                Filters.Group.allCreatures,
                Effects.ModifyStats(-2, -2, EffectTarget.IterationEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88bb420a-8bf1-4504-b1b5-2d929be978be.jpg"
        ruling(
            "2018-01-19",
            "If you cast a spell with ascend, you don't get the city's blessing until it resolves. Players may " +
                "respond to that spell by trying to change whether you get the city's blessing."
        )
        ruling(
            "2018-01-19",
            "Golden Demise affects only creatures that are on the battlefield at the time it resolves. Creatures " +
                "that enter the battlefield later in the turn won't get -2/-2. Similarly, if you have the city's " +
                "blessing, creatures that come under your opponents' control later in the turn won't get -2/-2."
        )
    }
}
