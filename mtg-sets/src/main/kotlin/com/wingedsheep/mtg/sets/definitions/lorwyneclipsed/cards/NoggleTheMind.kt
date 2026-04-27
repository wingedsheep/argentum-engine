package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.TransformPermanent

/**
 * Noggle the Mind
 * {1}{U}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Enchanted creature loses all abilities and is a colorless Noggle
 * with base power and toughness 1/1. (It loses all colors and all
 * other creature types.)
 */
val NoggleTheMind = card("Noggle the Mind") {
    manaCost = "{1}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\nEnchanted creature loses all abilities and is a colorless Noggle with base power and toughness 1/1. (It loses all colors and all other creature types.)"

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("CREATURE"),
            setSubtypes = setOf("Noggle"),
            setColors = emptySet(),
            target = StaticTarget.AttachedCreature
        )
    }

    staticAbility {
        ability = LoseAllAbilities(target = StaticTarget.AttachedCreature)
    }

    staticAbility {
        ability = SetBasePowerToughnessStatic(1, 1, target = StaticTarget.AttachedCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Thomas M. Baxa"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/076c0ac1-722e-49ef-b815-ace210069972.jpg?1767957020"
    }
}
