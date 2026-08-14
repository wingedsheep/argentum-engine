package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Sharae of Numbing Depths
 * {2}{W}{U}
 * Legendary Creature — Merfolk Wizard
 * 2/3
 *
 * When Sharae enters, tap target creature an opponent controls and put a stun counter on it.
 * Whenever you tap one or more untapped creatures your opponents control, draw a card. This ability
 * triggers only once each turn.
 *
 * The draw is the **batch** form of the tap-attribution trigger — `Triggers.YouTap(…, batch = true)`
 * (CR 603.2c): tapping several of your opponents' creatures at once (a sweeper tapper, one
 * resolution that taps two) is one event batch and draws one card, not one per creature. Only taps
 * *you* caused count toward the batch, so an opponent tapping their own creatures never fires it,
 * and neither does a spell you control that instructs *them* to tap (Tangle Wire).
 *
 * `oncePerTurn` carries the printed "triggers only once each turn" limit on top, which is what
 * separates this from Hylda of the Icy Crown / Icewrought Sentry / Solitary Sanctuary (per-tap
 * payoffs with no limit).
 */
val SharaeOfNumbingDepths = card("Sharae of Numbing Depths") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Merfolk Wizard"
    power = 2
    toughness = 3
    oracleText = "When Sharae enters, tap target creature an opponent controls and put a stun " +
        "counter on it. (If a permanent with a stun counter would become untapped, remove one from " +
        "it instead.)\n" +
        "Whenever you tap one or more untapped creatures your opponents control, draw a card. This " +
        "ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target(
            "target creature an opponent controls",
            TargetCreature(filter = TargetFilter.Creature.opponentControls())
        )
        effect = Effects.Tap(victim) then Effects.AddCounters(Counters.STUN, 1, victim)
        description = "When Sharae enters, tap target creature an opponent controls and put a stun " +
            "counter on it."
    }

    triggeredAbility {
        trigger = Triggers.YouTap(GameObjectFilter.Creature.opponentControls(), batch = true)
        oncePerTurn = true
        effect = Effects.DrawCards(1)
        description = "Whenever you tap one or more untapped creatures your opponents control, draw " +
            "a card. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/600bc36a-3ef0-459c-9a93-94ec45b8c3d9.jpg?1783915068"
        ruling(
            "2023-09-01",
            "You may target a creature that is already tapped with Sharae's first ability. If the " +
                "target creature is already tapped as it resolves, you will still put a stun counter " +
                "on it."
        )
        ruling(
            "2023-09-01",
            "Sharae of Numbing Depths's last ability will trigger only when an effect instructs you " +
                "to tap one or more creatures your opponents control. It won't trigger if a spell or " +
                "ability you control instructs an opponent to tap a creature they control. For " +
                "example, if you control Tangle Wire and an opponent taps an untapped creature they " +
                "control as part of the resolution of Tangle Wire's triggered ability, Sharae's " +
                "ability won't trigger."
        )
    }
}
