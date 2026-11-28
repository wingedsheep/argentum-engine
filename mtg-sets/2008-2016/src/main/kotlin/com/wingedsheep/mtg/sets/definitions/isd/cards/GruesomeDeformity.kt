package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gruesome Deformity
 * {B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has intimidate. (It can't be blocked except by artifact creatures and/or creatures that share a color with it.)
 */
val GruesomeDeformity = card("Gruesome Deformity") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has intimidate. (It can't be blocked except by artifact creatures and/or creatures that share a color with it.)"
    auraTarget = TargetObject(filter = TargetFilter.Creature)
    staticAbility {
        ability = GrantKeyword(Keyword.INTIMIDATE)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Matt Stewart"
        flavorText = "She wanted to inspire terror wherever she went. The demons happily granted her wish."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5696db03-206f-4e7e-9b65-ccef31bfd7d2.jpg"
    }
}
