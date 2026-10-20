package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Primordial Ooze
 * {R}
 * Creature — Ooze
 * 1/1
 * This creature attacks each combat if able.
 * At the beginning of your upkeep, put a +1/+1 counter on this creature. Then you may pay {X},
 * where X is the number of +1/+1 counters on it. If you don't, tap this creature and it deals X
 * damage to you.
 *
 * The upkeep trigger mirrors Cyclone: add the counter first, then an `Effects.MayPay` over a
 * generic `Effects.PayDynamicMana` whose amount is read after the counter lands. Declining taps the
 * Ooze and has it (the ability's source) deal X damage to its controller; the tap happens even if
 * that damage is prevented.
 */
val PrimordialOoze = card("Primordial Ooze") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ooze"
    oracleText = "This creature attacks each combat if able.\n" +
        "At the beginning of your upkeep, put a +1/+1 counter on this creature. Then you may pay {X}, " +
        "where X is the number of +1/+1 counters on it. If you don't, tap this creature and it deals " +
        "X damage to you."
    power = 1
    toughness = 1

    staticAbility {
        ability = MustAttack()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)

        val counters = DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE)

        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.MayPay(
                cost = Effects.PayDynamicMana(counters),
                then = Effects.Nothing,
                otherwise = Effects.Tap(EffectTarget.Self) then
                    Effects.DealDamage(counters, EffectTarget.PlayerRef(Player.You)),
            )
        description = "At the beginning of your upkeep, put a +1/+1 counter on this creature. Then you " +
            "may pay {X}, where X is the number of +1/+1 counters on it. If you don't, tap this " +
            "creature and it deals X damage to you."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Sandra Everingham"
        flavorText = "The thick, moving mass from the beginning of evolution swallows and digests the " +
            "animate and inanimate as it unthinkingly strives to continue its existence."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a46e47e1-8639-48f7-94c4-5f9e9666839a.jpg?1783948053"
        ruling("2004-10-04", "It becomes tapped even if the damage is prevented.")
    }
}
