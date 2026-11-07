package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Choking Restraints — Eldritch Moon #16
 * {2}{W} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't attack or block.
 * {3}{W}{W}, Sacrifice this Aura: Exile enchanted creature.
 *
 * The Pacifism idiom ([CantAttack] + [CantBlock] over the attached creature) plus Spiral into
 * Solitude's sacrifice-to-exile ability: the Aura is sacrificed as a cost, and
 * [EffectTarget.EnchantedCreature] resolves to the creature it was attached to.
 */
val ChokingRestraints = card("Choking Restraints") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block.\n" +
        "{3}{W}{W}, Sacrifice this Aura: Exile enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{W}{W}"), Costs.SacrificeSelf)
        effect = Effects.Exile(EffectTarget.EnchantedCreature)
        description = "{3}{W}{W}, Sacrifice this Aura: Exile enchanted creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4549735d-df63-47db-ad53-0497499fe387.jpg"
    }
}
