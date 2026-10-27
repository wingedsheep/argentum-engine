package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Luminarch Ascension — Zendikar #25
 * {1}{W} · Enchantment
 *
 * At the beginning of each opponent's end step, if you didn't lose life this turn, you may put a
 * quest counter on this enchantment. (Damage causes loss of life.)
 * {1}{W}: Create a 4/4 white Angel creature token with flying. Activate only if this enchantment
 * has four or more quest counters on it.
 *
 * The end-step trigger is an intervening "if" on the whole turn's life loss (rechecked on
 * resolution, per the ruling), read as "not (you lost life this turn)".
 */
val LuminarchAscension = card("Luminarch Ascension") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each opponent's end step, if you didn't lose life this turn, you may " +
        "put a quest counter on this enchantment. (Damage causes loss of life.)\n" +
        "{1}{W}: Create a 4/4 white Angel creature token with flying. Activate only if this enchantment " +
        "has four or more quest counters on it."

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.END)
        interveningIf = Conditions.Not(Conditions.YouLostLifeThisTurn)
        optional = true
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self)
        description = "At the beginning of each opponent's end step, if you didn't lose life this turn, " +
            "you may put a quest counter on this enchantment."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SourceCounterCountAtLeast(CounterType.QUEST, 4))
        )
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/1/f/1fc37484-b58b-4a42-9281-b4272dc7c872.jpg?1783942179"
        )
        description = "Create a 4/4 white Angel creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Michael Komarck"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9f3a20f-a788-4d8b-946b-d9c1c983a85a.jpg?1783942171"
        ruling("2018-03-16", "Luminarch Ascension's first ability doesn't trigger if you have lost life before an opponent's end step begins. If it does trigger but you lose life before it resolves, it does nothing and you won't put a quest counter on Luminarch Ascension.")
        ruling("2018-03-16", "Luminarch Ascension's first ability cares only whether you lost life this turn, even if Luminarch Ascension wasn't on the battlefield when that happened. It doesn't care how much you lost, whether you also gained life, or even whether you gained more life than you lost.")
        ruling("2018-03-16", "You can activate Luminarch Ascension's last ability during the end step in which it receives its fourth quest counter.")
    }
}
