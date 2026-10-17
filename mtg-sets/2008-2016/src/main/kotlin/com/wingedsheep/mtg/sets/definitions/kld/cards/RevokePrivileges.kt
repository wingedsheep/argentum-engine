package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Revoke Privileges
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't attack, block, or crew Vehicles.
 */
val RevokePrivileges = card("Revoke Privileges") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature can't attack, block, or crew Vehicles."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.CANT_CREW.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "James Ryman"
        flavorText = "\"By order of Consulate Mandate 482, I am required to detain you, citizen.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84e44502-f4e4-440a-92b1-97bfb6314820.jpg?1783937228"
    }
}
