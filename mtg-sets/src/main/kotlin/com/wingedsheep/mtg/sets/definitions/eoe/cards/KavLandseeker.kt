package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kav Landseeker
 * {3}{R}
 * Creature — Kavu Soldier
 * Menace
 * When this creature enters, create a Lander token. At the beginning of the end step on your next
 * turn, sacrifice that token.
 * 4/3
 */
val KavLandseeker = card("Kav Landseeker") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kavu Soldier"
    power = 4
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature enters, create a Lander token. At the beginning of the end step on your " +
        "next turn, sacrifice that token. (It's an artifact with \"{2}, {T}, Sacrifice this token: " +
        "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")"

    keywords(Keyword.MENACE)

    // ETB: create a Lander, then schedule a sacrifice on the controller's next end step.
    // PipelineTarget(CREATED_TOKENS, 0) addresses the just-created Lander; the delayed-trigger
    // executor bakes it into a concrete entity id so the trigger still resolves after the
    // pipeline context is gone. timing = NEXT_TURN defers past this turn no matter the
    // current step; fireOnlyOnControllersTurn = true lands the trigger on the controller's
    // upcoming turn rather than an intervening opponent turn.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                Effects.CreateLander(),
                CreateDelayedTriggerEffect(
                    step = Step.END,
                    effect = SacrificeTargetEffect(
                        target = EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
                    ),
                    fireOnlyOnControllersTurn = true,
                    timing = DelayedTriggerTiming.NEXT_TURN
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a5a7e89-50e3-43cb-af93-d7d80a630c11.jpg?1752947111"
    }
}
