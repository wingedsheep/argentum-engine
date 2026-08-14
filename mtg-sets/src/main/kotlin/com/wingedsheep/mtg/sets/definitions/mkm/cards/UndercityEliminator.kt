package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Undercity Eliminator — Murders at Karlov Manor #108
 * {3}{B}{B} · Creature — Gorgon Assassin · 3/3
 *
 * When this creature enters, you may sacrifice an artifact or creature. When you do, exile target
 * creature an opponent controls.
 *
 * "**When** you do" is the giveaway: this is a genuine CR 603.12 reflexive trigger, not an "if you
 * do" continuation, and Scryfall's ruling spells the consequence out — no target is chosen when the
 * enters trigger goes on the stack. The target is picked only once the sacrifice has happened, as
 * the reflexive ability goes on the stack, and opponents get priority to respond to *that*. So a
 * board with no legal opposing creature at ETB time is not a reason to skip the sacrifice, and an
 * opponent who kills their own creature in response to the reflexive can still fizzle the exile
 * after the artifact is already gone. [ReflexiveTriggerEffect]'s `reflexiveTargetRequirements`
 * models exactly that split.
 *
 * The action is a plain [SacrificeEffect] over `CreatureOrArtifact` rather than a cost rail: it is
 * an *effect* of a resolving ability, so nothing is paid on announcement. `optional = true` carries
 * the "you may", and the executor's feasibility check declines to prompt at all when the controller
 * has no artifact and no creature — which can genuinely happen, since the Eliminator itself is a
 * legal choice (the text says "an artifact or creature", not "another") and something may have
 * removed it before the trigger resolves.
 */
val UndercityEliminator = card("Undercity Eliminator") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Gorgon Assassin"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you may sacrifice an artifact or creature. When you " +
        "do, exile target creature an opponent controls."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = SacrificeEffect(filter = GameObjectFilter.CreatureOrArtifact),
            optional = true,
            reflexiveEffect = Effects.Exile(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(Targets.CreatureOpponentControls),
        )
        description = "When this creature enters, you may sacrifice an artifact or creature. " +
            "When you do, exile target creature an opponent controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Quintin Gleim"
        flavorText = "With their guild shunned for opening the doors to Phyrexia, some among the " +
            "Golgari embraced their status as pariahs and took out their bitterness on all who " +
            "crossed their paths."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a67a4c5e-215b-4f03-87f7-c1af4f9f0a63.jpg?1783912889"
        ruling(
            "2024-02-02",
            "You don't choose a target at the time Undercity Eliminator's ability triggers. " +
                "Rather, a second \"reflexive\" ability triggers when you sacrifice an artifact or " +
                "creature this way. You choose a target for that ability as it goes on the stack. " +
                "Each player may respond to this triggered ability as normal.",
        )
    }
}
