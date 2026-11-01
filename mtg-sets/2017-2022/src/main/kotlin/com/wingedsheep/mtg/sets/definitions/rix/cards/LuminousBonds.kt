package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Luminous Bonds
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack or block.
 */
val LuminousBonds = card("Luminous Bonds") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature can't attack or block."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Steve Argyle"
        flavorText = "\"Your part in this fight is done, vampire. Get used to your thirst.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2ca3a547-ef22-4b87-b90f-e8ffb42f73ac.jpg?1783935337"
    }
}
