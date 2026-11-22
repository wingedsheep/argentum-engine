package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Illusionary Armor — Magic 2014 #59 (canonical printing)
 * {4}{U} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +4/+4.
 * When enchanted creature becomes the target of a spell or ability, sacrifice this Aura.
 *
 * The trigger is Fractured Loyalty's ANY-bound `attachedToBySource()` becomes-target shape —
 * `BecomesTargetEvent` isn't routed through ATTACHED bindings — and it fires on any spell or
 * ability, including the Aura controller's own.
 */
val IllusionaryArmor = card("Illusionary Armor") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +4/+4.\n" +
        "When enchanted creature becomes the target of a spell or ability, sacrifice this Aura."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(4, 4, Filters.EnchantedCreature)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attachedToBySource()).becomesTarget()
        effect = SacrificeSelfEffect
        description = "When enchanted creature becomes the target of a spell or ability, sacrifice this Aura."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d09346ff-6e63-499b-9265-c15a7b2cdece.jpg?1783939933"
    }
}
