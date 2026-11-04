package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tilonalli's Crown
 * {1}{R}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, it deals 1 damage to enchanted creature.
 * Enchanted creature gets +3/+0 and has trample.
 */
val TilonallisCrown = card("Tilonalli's Crown") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, it deals 1 damage to enchanted creature.\n" +
        "Enchanted creature gets +3/+0 and has trample."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(1, EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = ModifyStats(3, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Suzanne Helmigh"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0e941a7-c2c1-4855-b79a-c0dfebb28854.jpg?1783935292"
    }
}
