package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Moat
 * {2}{W}{W}
 * Enchantment
 *
 * Creatures without flying can't attack.
 */
val Moat = card("Moat") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Creatures without flying can't attack."

    staticAbility {
        ability = CantAttack(GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/952ba126-0915-47f0-9b6a-a0a6dcd22c6f.jpg?1783948082"
    }
}
