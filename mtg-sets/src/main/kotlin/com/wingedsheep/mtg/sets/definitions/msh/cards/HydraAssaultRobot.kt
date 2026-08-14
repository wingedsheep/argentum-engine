package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * HYDRA Assault Robot — Marvel Super Heroes #137 (common)
 * {1}{R} · Artifact Creature — Robot Villain · 1/3
 *
 * Whenever another Villain and/or artifact you control enters, this creature deals 1 damage to
 * target opponent.
 *
 * "Villain and/or artifact" is one trigger with a union filter, not two abilities: a permanent
 * that is both (this robot's own kind) still fires it exactly once. The union is built with
 * `GameObjectFilter.or`, which — both branches sharing the same (absent) state/controller gate —
 * collapses to a single `CardPredicate.Or` before `.youControl()` applies the controller gate to
 * the whole thing. [TriggerBinding.OTHER] is the "another" clause, so the robot's own arrival
 * doesn't trigger it. Same shape as Yellowjacket, Heartless Marauder / Machinesmith Automaton,
 * with the two filters unioned.
 */
val HydraAssaultRobot = card("HYDRA Assault Robot") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Robot Villain"
    oracleText = "Whenever another Villain and/or artifact you control enters, this creature " +
        "deals 1 damage to target opponent."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = (
                GameObjectFilter.Permanent.withSubtype(Subtype.VILLAIN) or GameObjectFilter.Artifact
                ).youControl(),
            binding = TriggerBinding.OTHER
        )
        target = Targets.Opponent
        effect = Effects.DealDamage(1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Wero Gallo"
        flavorText = "\"I have come for you, Nick Fury!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3b23fff-7d3b-4341-b556-82136f8c113b.jpg?1783902930"
    }
}
