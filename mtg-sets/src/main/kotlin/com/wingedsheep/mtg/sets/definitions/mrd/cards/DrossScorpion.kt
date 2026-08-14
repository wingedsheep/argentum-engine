package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Dross Scorpion — Mirrodin #164
 * {4} · Artifact Creature — Scorpion · 3/1
 *
 * Whenever this creature or another artifact creature dies, you may untap target artifact.
 *
 * "This creature or another artifact creature" is [TriggerBinding.ANY] over an artifact-creature
 * filter — ANY already covers the source itself, so no separate self-trigger is needed. Any
 * controller's artifact creature counts; the oracle doesn't restrict it to yours.
 *
 * The Scorpion's own death fires this, which is the card's whole point in an untap-for-value deck:
 * the trigger is put on the stack from the graveyard as a leaves-the-battlefield trigger, so it
 * still resolves after the body is gone. The target is chosen as the trigger goes on the stack; the
 * "may" is a resolution-time decline ([MayEffect]), so declining still uses up the trigger.
 */
val DrossScorpion = card("Dross Scorpion") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Scorpion"
    power = 3
    toughness = 1
    oracleText = "Whenever this creature or another artifact creature dies, you may untap target artifact."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.ArtifactCreature,
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        val artifact = target("target artifact", TargetPermanent(filter = TargetFilter.Artifact))
        effect = MayEffect(Effects.Untap(artifact))
        description = "Whenever this creature or another artifact creature dies, you may untap target artifact."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Jim Nelson"
        flavorText = "They skitter out of the mists to consume fresh kill before Mephidross has a " +
            "chance to corrode it away."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27c5381c-2e79-41fa-b1d7-2cf0c6dc1808.jpg?1783944523"
    }
}
