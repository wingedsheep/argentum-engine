package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Snaremaster Sprite
 * {U}
 * Creature — Faerie Wizard
 * 1/1
 *
 * Flying
 * When this creature enters, you may pay {2}. When you do, tap target creature an opponent
 * controls and put a stun counter on it.
 *
 * The tap half is a *reflexive* triggered ability (CR 603.12): no target is chosen when the
 * enters trigger goes on the stack — only when the {2} has actually been paid (2023-09-01
 * ruling). The optional {2} payment ([Effects.MayPay], manual mana-source selection) gates an
 * [Effects.ReflexiveTrigger] whose half picks its target only after the payment.
 *
 * An already-tapped creature is a legal target and still gets the stun counter.
 */
val SnaremasterSprite = card("Snaremaster Sprite") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Wizard"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "When this creature enters, you may pay {2}. When you do, tap target creature an opponent " +
        "controls and put a stun counter on it. (If a permanent with a stun counter would become " +
        "untapped, remove one from it instead.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}"),
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                descriptionOverride = "tap target creature an opponent " +
                    "controls and put a stun counter on it.",
            ) {
                val t = target(TargetFilter.Creature.opponentControls())
                effect = Effects.Tap(t) then Effects.AddCounters(CounterType.STUN, 1, t)
            },
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eaa37390-5c32-46ae-89d1-c094c9aa01e5.jpg?1783915115"
        ruling(
            "2023-09-01",
            "You don't choose a target for Snaremaster Sprite's ability at the time it triggers. Rather, a " +
                "second \"reflexive\" ability triggers when you pay {2} this way. You choose a target for that " +
                "ability as it goes on the stack. Each player may respond to this triggered ability as normal."
        )
        ruling(
            "2023-09-01",
            "You may target a creature that is already tapped with the reflexive triggered ability. If the " +
                "target creature is already tapped as the ability resolves, you will still put a stun counter on it."
        )
    }
}
