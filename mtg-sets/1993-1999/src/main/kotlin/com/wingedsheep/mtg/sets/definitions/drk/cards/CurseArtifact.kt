package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Curse Artifact
 * {2}{B}{B}
 * Enchantment — Aura
 * Enchant artifact
 * At the beginning of the upkeep of enchanted artifact's controller, this Aura deals 2 damage to
 * that player unless they sacrifice that artifact.
 *
 * Erosion's black cousin, on the same two pieces: an ATTACHED-bound step trigger — timed off the
 * *enchanted permanent's* controller, who is bound as the triggering player — feeding a
 * `PayOrSufferEffect` whose payer and damage recipient are both that `TriggeringPlayer`. The Aura's
 * controller controls the ability itself.
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
    auraTarget = TargetObject(filter = TargetFilter.Artifact)

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Artifact.attachedToBySource()),
            suffer = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
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
