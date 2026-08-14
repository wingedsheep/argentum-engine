package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Peter Parker's Camera (Marvel's Spider-Man, #171)
 * {1}
 * Artifact
 *
 * This artifact enters with three film counters on it.
 * {2}, {T}, Remove a film counter from this artifact: Copy target activated or triggered ability
 * you control. You may choose new targets for the copy.
 *
 * Implementation (fully built from composable primitives — no card-specific engine code):
 *  - Enters-with-counters: [EntersWithCounters] replacement (`selfOnly = true`,
 *    `CounterTypeFilter.Named(Counters.FILM)`, count 3) — applied as the Camera enters, so it works
 *    on cast entry and any other battlefield entry (same shape as Braided Net / Wishclaw Talisman).
 *    `Counters.FILM` / `CounterType.FILM` are a new passive "uses left" named counter (same pattern
 *    as `net` / `wish` / `ingenuity`), so the three counters bound how many times the copy ability
 *    can be used before the Camera sits inert.
 *  - Copy ability: cost `Costs.Composite(Costs.Mana("{2}"), Costs.Tap,
 *    Costs.RemoveCounterFromSelf(Counters.FILM, 1))` — "{2}, {T}, Remove a film counter". The target
 *    is [Targets.ActivatedOrTriggeredAbilityYouControl] (an activated or triggered ability you
 *    control on the stack; mana abilities never use the stack, so they're excluded automatically),
 *    and [Effects.CopyTargetSpellOrAbility] dispatches on the chosen object and copies its
 *    ability-on-stack component, reprompting for new targets per CR 707.10c (or copying a no-target
 *    ability without a prompt). Same primitive as Return the Favor's copy mode and The Enigma
 *    Jewel's back-face copy trigger.
 */
val PeterParkersCamera = card("Peter Parker's Camera") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters with three film counters on it.\n" +
        "{2}, {T}, Remove a film counter from this artifact: Copy target activated or triggered " +
        "ability you control. You may choose new targets for the copy."

    // This artifact enters with three film counters on it.
    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.FILM),
            count = 3,
            selfOnly = true
        )
    )

    // {2}, {T}, Remove a film counter from this artifact: Copy target activated or triggered
    // ability you control. You may choose new targets for the copy.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(Counters.FILM, 1)
        )
        val copied = target(
            "activated or triggered ability you control",
            Targets.ActivatedOrTriggeredAbilityYouControl
        )
        effect = Effects.CopyTargetSpellOrAbility(copied)
        description = "{2}, {T}, Remove a film counter from this artifact: Copy target activated or " +
            "triggered ability you control. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Lixin Yin"
        flavorText = "\"How does Parker get those close-ups?\"\n—J. Jonah Jameson"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47875dff-c046-4cb0-b1e3-f926cbe25b59.jpg?1783905303"
    }
}
