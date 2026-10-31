package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Savage Gorger (M20 #291)
 * {1}{B}{B}  Creature — Vampire  1/1
 *
 * Flying
 * At the beginning of your end step, if an opponent lost life this turn, put a +1/+1 counter on
 * this creature. (Damage causes loss of life.)
 */
val SavageGorger = card("Savage Gorger") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    oracleText = "Flying\n" +
        "At the beginning of your end step, if an opponent lost life this turn, put a +1/+1 counter on this creature. " +
        "(Damage causes loss of life.)"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.OpponentLostLifeThisTurn
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "291"
        artist = "Antonio José Manzanedo"
        flavorText = "\"I have no patience for sipping blood from crystal goblets. I want to bite and tear.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a25ea81f-9786-4622-978d-b082fb72622c.jpg"
    }
}
