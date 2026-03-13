package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantColor
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic

/**
 * Deep Freeze
 * {2}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has base power and toughness 0/4, has defender, loses all other
 * abilities, and is a blue Wall in addition to its other colors and types.
 */
val DeepFreeze = card("Deep Freeze") {
    manaCost = "{2}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has base power and toughness 0/4, has defender, loses all other abilities, and is a blue Wall in addition to its other colors and types."

    auraTarget = Targets.Creature

    staticAbility {
        ability = SetBasePowerToughnessStatic(0, 4)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.DEFENDER)
    }

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = GrantSubtype("Wall")
    }

    staticAbility {
        ability = GrantColor(Color.BLUE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Svetlin Velinov"
        flavorText = "\"For cryomancers, studying the Ice Age isn't just an academic exercise.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51765d87-e842-4d84-aaf0-998737fe754c.jpg?1562735580"
    }
}
