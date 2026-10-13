package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * In Thrall to the Pit
 * {3}{R}
 * Sorcery
 * Kicker {2}{B} (You may pay an additional {2}{B} as you cast this spell.)
 * Gain control of target creature until end of turn. Untap that creature. It gains haste
 * until end of turn. If this spell was kicked, sacrifice that creature at the beginning of
 * the next end step.
 *
 * The kicked rider creates a delayed end-step trigger that sacrifices the stolen creature.
 * `SacrificeTarget` only sacrifices a permanent on the battlefield that the trigger's controller
 * still controls, so the ruling's end-step-cast case (control already lost when the trigger
 * resolves) no-ops, as does a stolen creature that was flickered back to its owner.
 */
val InThrallToThePit = card("In Thrall to the Pit") {
    manaCost = "{3}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Kicker {2}{B} (You may pay an additional {2}{B} as you cast this spell.)\n" +
        "Gain control of target creature until end of turn. Untap that creature. It gains haste " +
        "until end of turn. If this spell was kicked, sacrifice that creature at the beginning of " +
        "the next end step."

    keywordAbility(KeywordAbility.kicker("{2}{B}"))

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.If(
                condition = Conditions.WasKicked,
                then = Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.SacrificeTarget(t),
                ),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f33284f-bdb1-4982-8636-8f2e12896053.jpg?1783921315"
        ruling(
            "2022-09-09",
            "If an effect allows In Thrall to the Pit to be cast during the end step and it is kicked, " +
                "you will gain control of the target creature until this turn ends. The delayed triggered " +
                "ability will trigger at the beginning of the next turn's end step, but you won't be able " +
                "to sacrifice that creature because you no longer control it."
        )
    }
}
