package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bower Passage
 * {1}{G}
 * Enchantment
 * Creatures with flying can't block creatures you control.
 */
val BowerPassage = card("Bower Passage") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Creatures with flying can't block creatures you control."

    staticAbility {
        ability = CantBeBlockedBy(
            blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9f0048f-aaa0-4597-b898-ee754d0bbe4b.jpg?1783940670"
    }
}
