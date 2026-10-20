package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Infernal Medusa
 * {3}{B}{B}
 * Creature — Gorgon
 * 2/4
 * Whenever this creature blocks a creature, destroy that creature at end of combat.
 * Whenever this creature becomes blocked by a non-Wall creature, destroy that creature at end of
 * combat.
 *
 * Two asymmetric halves of Thicket Basilisk: the blocking half has no Wall exclusion (an attacking
 * Wall is still destroyed). `blocks(Creature)` fires once per blocked attacker and
 * `becomesBlocked(by)` once per matching blocker, each with the combat partner as
 * `TriggeringEntity`, and each schedules a delayed end-of-combat destroy. Untargeted.
 */
val InfernalMedusa = card("Infernal Medusa") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Gorgon"
    power = 2
    toughness = 4
    oracleText = "Whenever this creature blocks a creature, destroy that creature at end of combat.\n" +
        "Whenever this creature becomes blocked by a non-Wall creature, destroy that creature at end of combat."

    triggeredAbility {
        trigger = Triggers.self.blocks(GameObjectFilter.Creature)
        effect = Effects.CreateDelayedTrigger(
            step = Step.END_COMBAT,
            effect = Effects.Destroy(EffectTarget.TriggeringEntity),
        )
        description = "Whenever this creature blocks a creature, destroy that creature at end of combat."
    }

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked(GameObjectFilter.Creature.notSubtype(Subtype.WALL))
        effect = Effects.CreateDelayedTrigger(
            step = Step.END_COMBAT,
            effect = Effects.Destroy(EffectTarget.TriggeringEntity),
        )
        description = "Whenever this creature becomes blocked by a non-Wall creature, destroy that " +
            "creature at end of combat."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26a5333f-2761-42b8-ae8b-1d360b109daf.jpg?1783948064"
    }
}
