package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype

/**
 * Clutch of Undeath
 * {3}{B}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +3/+3 as long as it's a Zombie. Otherwise, it gets -3/-3.
 */
val ClutchOfUndeath = card("Clutch of Undeath") {
    manaCost = "{3}{B}{B}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +3/+3 as long as it's a Zombie. Otherwise, it gets -3/-3."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(3, 3, StaticTarget.AttachedCreature)
        condition = EnchantedCreatureHasSubtype(Subtype("Zombie"))
    }

    staticAbility {
        ability = ModifyStats(-3, -3, StaticTarget.AttachedCreature)
        condition = Conditions.Not(EnchantedCreatureHasSubtype(Subtype("Zombie")))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Greg Hildebrandt"
        flavorText = "\"The hand of death recognizes its own.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7301fdec-ca17-47ae-9a0a-84ea8665ece1.jpg?1562530624"
    }
}
