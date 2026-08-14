package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Coalstoke Gearhulk
 * {1}{B}{B}{R}{R}
 * Artifact Creature — Construct
 * 5/4
 * Menace, deathtouch
 * When this creature enters, put target creature card with mana value 4 or less from a graveyard
 * onto the battlefield under your control with a finality counter on it. That creature gains
 * menace, deathtouch, and haste. At the beginning of your next end step, exile that creature.
 *
 * "With a finality counter on it" is the *entry* rider, so the counter rides along on the
 * zone-change ([Effects.Move]'s `addCounterType`) rather than being added afterwards — a permanent
 * that dies in the same window is still exiled instead. The same move needs an explicit
 * `controllerOverride`: a bare move to the battlefield hands the card back to its **owner**, which
 * is wrong the moment the target sits in an opponent's graveyard.
 *
 * The keyword grants use [Duration.Permanent]: the oracle text gives no duration, so they last as
 * long as the creature is on the battlefield. The delayed exile is gated to the controller's own
 * end step ([DelayedTriggerTiming.NEXT_END_STEP] + `fireOnPlayer = You`) — "your next end step"
 * still means this turn's if it hasn't begun yet.
 */
val CoalstokeGearhulk = card("Coalstoke Gearhulk") {
    manaCost = "{1}{B}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Artifact Creature — Construct"
    oracleText = "Menace, deathtouch\n" +
        "When this creature enters, put target creature card with mana value 4 or less from a " +
        "graveyard onto the battlefield under your control with a finality counter on it. That " +
        "creature gains menace, deathtouch, and haste. At the beginning of your next end step, " +
        "exile that creature."
    power = 5
    toughness = 4

    keywords(Keyword.MENACE, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val reanimated = target(
            "target creature card with mana value 4 or less in a graveyard",
            TargetObject(filter = TargetFilter.CreatureInGraveyard.manaValueAtMost(4))
        )
        effect = Effects.Composite(
            Effects.Move(
                reanimated,
                Zone.BATTLEFIELD,
                controllerOverride = EffectTarget.Controller,
                addCounterType = CounterType.FINALITY
            ),
            Effects.GrantKeyword(Keyword.MENACE, reanimated, Duration.Permanent),
            Effects.GrantKeyword(Keyword.DEATHTOUCH, reanimated, Duration.Permanent),
            Effects.GrantKeyword(Keyword.HASTE, reanimated, Duration.Permanent),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = Effects.Exile(reanimated),
                timing = DelayedTriggerTiming.NEXT_END_STEP,
                fireOnPlayer = EffectTarget.PlayerRef(Player.You)
            )
        )
        description = "When this creature enters, put target creature card with mana value 4 or " +
            "less from a graveyard onto the battlefield under your control with a finality " +
            "counter on it. That creature gains menace, deathtouch, and haste. At the beginning " +
            "of your next end step, exile that creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "198"
        artist = "Nino Vecia"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73431628-b9b0-41e6-8e9b-8a090939b0c1.jpg?1783907861"
        ruling(
            "2025-02-07",
            "Finality counters work on any permanent, not only creatures. If a permanent with a " +
                "finality counter on it would be put into a graveyard from the battlefield, exile " +
                "it instead."
        )
        ruling("2025-02-07", "Multiple finality counters on a single permanent are redundant.")
    }
}
