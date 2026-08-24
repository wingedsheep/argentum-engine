package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Curse Artifact
 * {2}{B}{B}
 * Enchantment — Aura
 * Enchant artifact
 * At the beginning of the upkeep of enchanted artifact's controller, this Aura deals 2 damage to
 * that player unless they sacrifice that artifact.
 *
 * Erosion's black cousin, on the same two pieces: an ATTACHED-bound step trigger — which the engine
 * resolves against the *enchanted permanent's* controller and makes that player the ability's
 * controller — feeding a `PayOrSufferEffect` whose default payer is therefore already the right
 * player.
 *
 * The escape is "sacrifice **that** artifact", not any artifact, so the cost filter is
 * `attachedToBySource()`: the one permanent this Aura is attached to. A plain artifact filter would
 * let the victim feed it something worthless and keep the cursed one.
 *
 * The damage source is the Aura itself, which is what the printed text says and what protection and
 * prevention effects will read.
 */
val CurseArtifact = card("Curse Artifact") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\n" +
        "At the beginning of the upkeep of enchanted artifact's controller, this Aura deals 2 " +
        "damage to that player unless they sacrifice that artifact."
    auraTarget = Targets.Artifact

    triggeredAbility {
        trigger = Triggers.phase(Step.UPKEEP, binding = TriggerBinding.ATTACHED)
        effect = PayOrSufferEffect(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Artifact.attachedToBySource()),
            suffer = Effects.DealDamage(2, EffectTarget.Controller),
        )
        description = "At the beginning of the upkeep of enchanted artifact's controller, this " +
            "Aura deals 2 damage to that player unless they sacrifice that artifact."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Mark Tedin"
        flavorText = "Voska feared the artifact had come too easily."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fc0d070-8a42-4d5e-8f2b-ceb59147de6f.jpg?1783947940"
    }
}
