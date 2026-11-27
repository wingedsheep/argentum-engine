package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spectral Prison — Avacyn Restored #75 (canonical printing)
 * {1}{U} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature doesn't untap during its controller's untap step.
 * When enchanted creature becomes the target of a spell, sacrifice this Aura.
 *
 * Claustrophobia's lock without the enters-tap, plus Illusionary Armor's ANY-bound
 * `attachedToBySource()` becomes-target sacrifice, narrowed to spells only.
 */
val SpectralPrison = card("Spectral Prison") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature doesn't untap during its controller's untap step.\n" +
        "When enchanted creature becomes the target of a spell, sacrifice this Aura."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attachedToBySource()).becomesTarget(spellsOnly = true)
        effect = SacrificeSelfEffect
        description = "When enchanted creature becomes the target of a spell, sacrifice this Aura."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89d141bc-7307-40c2-a7ed-427caaec5efc.jpg?1783940711"
    }
}
