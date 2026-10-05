package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

val Burrowing = card("Burrowing") {
    manaCost = "{R}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has mountainwalk. (It can't be blocked as long as defending player controls a Mountain.)"
    colorIdentity = "R"
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.MOUNTAINWALK)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a14c05e4-8df3-450b-8a98-5028e73b14c1.jpg?1783948689"
    }
}
