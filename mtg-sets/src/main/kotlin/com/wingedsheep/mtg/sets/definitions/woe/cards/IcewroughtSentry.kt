package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Icewrought Sentry
 * {2}{U}
 * Creature — Elemental Soldier
 * 2/3
 *
 * Vigilance
 * Whenever this creature attacks, you may pay {1}{U}. When you do, tap target creature an opponent
 * controls.
 * Whenever you tap an untapped creature an opponent controls, this creature gets +2/+1 until end of
 * turn.
 *
 * The attack ability is a "When you do" reflexive (CR 603.12): no target is chosen when the attack
 * trigger goes on the stack — only once the {1}{U} is actually paid ([ReflexiveTriggerEffect]).
 *
 * The pump is [Triggers.YouTap] — tap *attribution*, so only a tap this creature's controller caused
 * fires it (not an opponent tapping their own creature to attack or crew). Its own reflexive tap is
 * one, so attacking and paying pumps it before damage; so does any other tapper you control, which
 * is why the trigger is a separate ability rather than a rider on the reflexive effect. Per-tap, not
 * once per turn: tapping two of an opponent's creatures at once pumps it twice.
 */
val IcewroughtSentry = card("Icewrought Sentry") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Soldier"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever this creature attacks, you may pay {1}{U}. When you do, tap target creature an " +
        "opponent controls.\n" +
        "Whenever you tap an untapped creature an opponent controls, this creature gets +2/+1 until " +
        "end of turn."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = ReflexiveTriggerEffect(
            action = PayManaCostEffect(ManaCost.parse("{1}{U}")),
            optional = true,
            reflexiveEffect = Effects.Tap(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetCreature(filter = TargetFilter.Creature.opponentControls())
            )
        )
        description = "Whenever this creature attacks, you may pay {1}{U}. When you do, tap target " +
            "creature an opponent controls."
    }

    triggeredAbility {
        trigger = Triggers.YouTap(GameObjectFilter.Creature.opponentControls())
        effect = Effects.ModifyStats(2, 1, EffectTarget.Self, Duration.EndOfTurn)
        description = "Whenever you tap an untapped creature an opponent controls, this creature " +
            "gets +2/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/859419d3-cd15-4362-98b3-a7ff98e29692.jpg?1783915119"
        ruling(
            "2023-09-01",
            "Icewrought Sentry's last ability will trigger only when an effect instructs you to tap " +
                "an opponent's creature. It won't trigger if a spell or ability you control instructs " +
                "an opponent to tap a creature they control. For example, if you control Tangle Wire " +
                "and an opponent taps an untapped creature they control as part of the resolution of " +
                "Tangle Wire's triggered ability, Icewrought Sentry's ability won't trigger."
        )
    }
}
