package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skirk Alarmist
 * {1}{R}
 * Creature — Human Wizard
 * 1/2
 * Haste
 * {T}: Turn target face-down creature you control face up. At the beginning of
 * the next end step, sacrifice it.
 *
 * Ruling (2004-10-04): If this ability is used during the end step, the creature
 * is not sacrificed until the end of the next turn.
 */
val SkirkAlarmist = card("Skirk Alarmist") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "Haste\n{T}: Turn target face-down creature you control face up. At the beginning of the next end step, sacrifice it."

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Tap
        val t = target("target face-down creature you control", Targets.FaceDownCreatureYouControl)
        effect = TurnFaceUpEffect(t)
            .then(CreateDelayedTriggerEffect(
                step = Step.END,
                effect = SacrificeTargetEffect(t)
            ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Justin Sweet"
        flavorText = "\"I treat each day as your last.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd1c1d41-8666-4c1d-9498-0e259472958d.jpg?1562946070"
    }
}
