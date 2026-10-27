package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Infernal Pet
 * {2}{B}
 * Creature — Imp
 * 2/2
 * Whenever you cast your second spell each turn, put a +1/+1 counter on this creature and it gains
 * flying until end of turn.
 */
val InfernalPet = card("Infernal Pet") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Imp"
    oracleText = "Whenever you cast your second spell each turn, put a +1/+1 counter on this creature " +
        "and it gains flying until end of turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.castsNth(2)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "YW Tang"
        flavorText = "\"Here, my pet. Another little light for you to snuff out.\"\n—Kzarakh, Immersturm gamekeeper"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80340582-655b-4bf1-88fb-14bcbe17aa04.jpg?1783928245"
        ruling(
            "2021-02-05",
            "The triggered ability can trigger only once each turn. The ability will resolve before the second spell does. " +
                "It doesn't matter if the first spell you cast that turn has resolved, was countered, or is still on the stack."
        )
        ruling(
            "2021-02-05",
            "The triggered ability triggers only if the creature that has it is on the battlefield as you cast your second spell. " +
                "Spells you cast in a turn before that creature entered the battlefield will count. In other words, the ability " +
                "won't trigger if the creature with the ability is the second spell you cast during a turn or if you have already " +
                "cast two or more spells by the time that creature enters the battlefield that turn."
        )
    }
}
