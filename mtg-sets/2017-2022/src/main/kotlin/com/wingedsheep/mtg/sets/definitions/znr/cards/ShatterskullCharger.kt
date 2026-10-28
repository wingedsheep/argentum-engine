package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shatterskull Charger — Zendikar Rising #159
 * {1}{R}{R} · Creature — Giant Warrior · 4/3
 *
 * Kicker {2}
 * Trample, haste
 * If this creature was kicked, it enters with a +1/+1 counter on it.
 * At the beginning of your end step, if this creature doesn't have a +1/+1 counter on it, return
 * it to its owner's hand.
 *
 * The end-step return is an intervening-if trigger: it checks for a +1/+1 counter both when the
 * step begins and on resolution, so a counter gained in response keeps the Giant on the battlefield.
 */
val ShatterskullCharger = card("Shatterskull Charger") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Warrior"
    power = 4
    toughness = 3
    oracleText = "Kicker {2} (You may pay an additional {2} as you cast this spell.)\n" +
        "Trample, haste\n" +
        "If this creature was kicked, it enters with a +1/+1 counter on it.\n" +
        "At the beginning of your end step, if this creature doesn't have a +1/+1 counter on it, " +
        "return it to its owner's hand."

    keywordAbility(KeywordAbility.kicker("{2}"))
    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            selfOnly = true,
            condition = WasKicked,
        )
    )

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE))
        effect = Effects.ReturnToHand(EffectTarget.Self)
        description = "At the beginning of your end step, if this creature doesn't have a +1/+1 " +
            "counter on it, return it to its owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c6d6bc3-2162-4c2b-a5de-25103e706e37.jpg?1783929353"
    }
}
