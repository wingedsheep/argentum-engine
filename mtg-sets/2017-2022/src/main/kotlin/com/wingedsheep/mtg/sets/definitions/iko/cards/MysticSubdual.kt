package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mystic Subdual
 * {1}{U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature gets -2/-0 and loses all abilities. (Mutating onto the creature won't give it
 * new abilities. It can gain abilities in other ways.)
 */
val MysticSubdual = card("Mystic Subdual") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\nEnchanted creature gets -2/-0 and loses all abilities. " +
        "(Mutating onto the creature won't give it new abilities. It can gain abilities in other ways.)"

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-2, 0, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = LoseAllAbilities()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/627c2111-b7d7-40bc-aa30-57d72338b32d.jpg?1783931073"
    }
}
