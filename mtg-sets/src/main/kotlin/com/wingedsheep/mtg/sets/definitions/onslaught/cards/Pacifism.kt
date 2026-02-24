package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Pacifism
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack or block.
 */
val Pacifism = card("Pacifism") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature can't attack or block."

    auraTarget = Targets.Creature

    staticAbility {
        ability = CantAttack(target = StaticTarget.AttachedCreature)
    }

    staticAbility {
        ability = CantBlock(target = StaticTarget.AttachedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Matthew D. Wilson"
        flavorText = "Even those born to battle could only lay their blades at Akroma’s feet."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee262fde-8df1-431f-9e5c-0cafe9212b49.jpg?1562951573"
    }
}
