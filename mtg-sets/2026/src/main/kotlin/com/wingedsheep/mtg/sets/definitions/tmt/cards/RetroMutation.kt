package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Retro-Mutation
 * {2}{U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature is a Turtle with base power and toughness 0/1. It can't
 * attack and loses all abilities. (It also loses all other creature types.)
 */
val RetroMutation = card("Retro-Mutation") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\nEnchanted creature is a Turtle with base power and toughness 0/1. It can't attack and loses all abilities. (It also loses all other creature types.)"

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // "is a Turtle" — set its only creature subtype to Turtle (keeps the Creature type).
    staticAbility {
        ability = TransformPermanent(setCreatureSubtypes = setOf("Turtle"))
    }
    staticAbility {
        ability = SetBasePowerToughnessStatic(0, 1)
    }
    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = LoseAllAbilities()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Leonardo Santanna"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4af284ba-fa54-43c5-8c76-3e1128957452.jpg?1771586820"
    }
}
