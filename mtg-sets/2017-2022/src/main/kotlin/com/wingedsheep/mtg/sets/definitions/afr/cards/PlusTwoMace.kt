package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * +2 Mace
 * {1}{W}
 * Artifact — Equipment
 * Equipped creature gets +2/+2.
 * Equip {3}
 */
val PlusTwoMace = card("+2 Mace") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2.\nEquip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"
    staticAbility {
        ability = ModifyStats(2, 2)
    }
    equipAbility("{3}")
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Jarel Threat"
        flavorText = "The weight of this magic weapon falls heavy on the wicked."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e882c9f9-bf30-46b6-bedc-379d2c80e5cb.jpg?1783926539"
    }
}
