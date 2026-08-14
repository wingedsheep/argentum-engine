package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Cornered Crook — Murders at Karlov Manor #120
 * {4}{R} · Creature — Lizard Warrior · 5/4
 *
 * When this creature enters, you may sacrifice an artifact. When you do, this creature deals 3
 * damage to any target.
 *
 * A [ReflexiveTriggerEffect], and the reflexive half genuinely matters here — the Scryfall ruling is
 * explicit that no target is chosen when the enters ability triggers. The damage is a *second*
 * ability (CR 603.12) that goes on the stack only after the artifact is actually sacrificed, picks
 * its target then, and can be responded to on its own. Resolving the damage inline would let the
 * Crook's controller see the target before committing the artifact and would deny opponents the
 * priority window, both of which the rules forbid.
 *
 * The sacrifice is a resolution-time *choice*, not a target — `SelectTargetEffect` prompts for an
 * artifact its controller owns and `SacrificeTarget` sacrifices it. Any artifact qualifies, including
 * an artifact creature and including Clue tokens, which is the intended Karlov Manor synergy; the
 * Crook itself is not an artifact, so no `.other()` guard is needed.
 *
 * `optional = true` carries the "you may". With no artifact on the battlefield
 * `ReflexiveTriggerEffectExecutor.isActionFeasible` skips the whole thing rather than asking a
 * question that could only produce a vacuous yes.
 */
val CorneredCrook = card("Cornered Crook") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Warrior"
    oracleText = "When this creature enters, you may sacrifice an artifact. When you do, this " +
        "creature deals 3 damage to any target."
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = Effects.Composite(
                listOf(
                    SelectTargetEffect(
                        requirement = TargetObject(filter = TargetFilter.Artifact.youControl()),
                        storeAs = "toSacrifice"
                    ),
                    Effects.SacrificeTarget(EffectTarget.PipelineTarget("toSacrifice"))
                )
            ),
            optional = true,
            reflexiveEffect = Effects.DealDamage(3, EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(Targets.Any),
            descriptionOverride = "You may sacrifice an artifact. When you do, this creature deals " +
                "3 damage to any target."
        )
        description = "When this creature enters, you may sacrifice an artifact. When you do, this " +
            "creature deals 3 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Gabor Szikszai"
        flavorText = "Monax knew attacking the detective was a reckless move, but he was at the end " +
            "of his rope."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3aff1ea-1d25-49c3-a2d9-f435124a5969.jpg?1783912883"

        ruling(
            "2024-02-02",
            "You don't choose a target for Cornered Crook's ability at the time it triggers. Rather, " +
                "a second \"reflexive\" ability triggers when you sacrifice an artifact this way. You " +
                "choose a target for that ability as it goes on the stack. Each player may respond to " +
                "this triggered ability as normal."
        )
    }
}
