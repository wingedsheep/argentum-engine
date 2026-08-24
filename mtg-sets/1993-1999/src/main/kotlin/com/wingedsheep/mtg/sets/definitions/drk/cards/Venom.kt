package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Venom
 * {1}{G}{G}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature blocks or becomes blocked by a non-Wall creature, destroy the other
 * creature at end of combat.
 *
 * Serpentine Basilisk's shape moved onto an Aura: the combat trigger schedules a delayed
 * end-of-combat destroy aimed at the triggering entity, which for
 * [Triggers.BlocksOrBecomesBlockedBy] is the combat partner — "the *other* creature".
 *
 * The kill is deliberately delayed rather than immediate, so the poisoned creature still deals and
 * receives combat damage first; that is what makes Venom a deterrent rather than a removal spell.
 *
 * The Wall exclusion is a subtype test on the partner, so the Aura simply doesn't trigger against a
 * Wall — the printed card gives Walls no shield, it just never notices them.
 */
val Venom = card("Venom") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature blocks or becomes blocked by a non-Wall creature, destroy " +
        "the other creature at end of combat."
    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.BlocksOrBecomesBlockedBy(
            GameObjectFilter.Creature.notSubtype(Subtype.WALL),
            binding = TriggerBinding.ATTACHED,
        )
        effect = CreateDelayedTriggerEffect(
            step = Step.END_COMBAT,
            effect = Effects.Destroy(EffectTarget.TriggeringEntity),
        )
        description = "Whenever enchanted creature blocks or becomes blocked by a non-Wall " +
            "creature, destroy the other creature at end of combat."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Tom Wänerstrand"
        flavorText = "\"I told him it was just a flesh wound, a wee scratch, but the next time I " +
            "looked at him, poor Tadhg was dead and gone.\" —Maeveen O'Donagh, *Memoirs of a Soldier*"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb0480f5-6aae-4297-afa6-3f7a5801bf95.jpg?1783947928"
    }
}
