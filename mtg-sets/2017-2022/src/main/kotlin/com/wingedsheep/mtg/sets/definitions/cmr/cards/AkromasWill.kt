package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Akroma's Will — Commander Legends #3 (canonical printing)
 * {3}{W} · Instant
 *
 * Choose one. If you control a commander as you cast this spell, you may choose both instead.
 * • Creatures you control gain flying, vigilance, and double strike until end of turn.
 * • Creatures you control gain lifelink, indestructible, and protection from each color until end
 *   of turn.
 *
 * The conditional mode count is Jeska's Will's cast-time `dynamicChooseCount` over
 * [Conditions.YouControlACommander] (any player's commander counts; checked as the spell is cast).
 * Each mode is one pass over the creatures you control at resolution, granting all of its
 * abilities to each; "protection from each color" is the five per-color grants Sygg, Wanderbrine
 * Shield composes (there is no single "from each color" protection quality).
 */
val AkromasWill = card("Akroma's Will") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one. If you control a commander as you cast this spell, you may choose both instead.\n" +
        "• Creatures you control gain flying, vigilance, and double strike until end of turn.\n" +
        "• Creatures you control gain lifelink, indestructible, and protection from each color until end of turn."

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.YouControlACommander,
                ifTrue = 2,
                ifFalse = 1
            )
        ) {
            mode("Creatures you control gain flying, vigilance, and double strike until end of turn") {
                effect = Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    Effects.GrantKeyword(Keyword.FLYING, EffectTarget.IterationEntity) then
                        Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity) then
                        Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.IterationEntity),
                )
            }
            mode("Creatures you control gain lifelink, indestructible, and protection from each color until end of turn") {
                effect = Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.IterationEntity) then
                        Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity) then
                        Effects.GrantProtectionFromColor(Color.WHITE, EffectTarget.IterationEntity) then
                        Effects.GrantProtectionFromColor(Color.BLUE, EffectTarget.IterationEntity) then
                        Effects.GrantProtectionFromColor(Color.BLACK, EffectTarget.IterationEntity) then
                        Effects.GrantProtectionFromColor(Color.RED, EffectTarget.IterationEntity) then
                        Effects.GrantProtectionFromColor(Color.GREEN, EffectTarget.IterationEntity),
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c281997b-1566-4469-a14c-6645f81ab023.jpg?1783928892"
        ruling(
            "2025-11-17",
            "\"Protection from each color\" is shorthand for protection from white, from blue, from black, from red, and from green. Colorless is not a color.",
        )
        ruling("2020-11-10", "There's no extra bonus if you control more than one commander.")
        ruling(
            "2020-11-10",
            "Once you've announced that you're casting a spell, players can't take any actions until you've finished doing so. Notably, opponents can't try to remove your commander to change how many modes you may choose.",
        )
        ruling(
            "2020-11-10",
            "Even though the card is named after a specific character, controlling any commander will satisfy its condition.",
        )
        ruling("2020-11-10", "The commander you control doesn't have to be your commander.")
        ruling(
            "2020-11-10",
            "Once you've chosen both modes for the spell, it doesn't matter whether you continue to control a commander. This is true even if you somehow no longer control a commander as you finish casting the spell.",
        )
    }
}
