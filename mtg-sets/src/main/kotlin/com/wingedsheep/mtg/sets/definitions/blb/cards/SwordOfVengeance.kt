package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sword of Vengeance
 * {3}
 * Artifact — Equipment
 *
 * Equipped creature gets +2/+0 and has first strike, vigilance, trample, and haste.
 * Equip {3}
 */
val SwordOfVengeance = card("Sword of Vengeance") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0 and has first strike, vigilance, trample, and haste.\nEquip {3}"

    staticAbility {
        effect = Effects.ModifyStats(+2, 0)
        filter = Filters.EquippedCreature
    }

    staticAbility {
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE)
        filter = Filters.EquippedCreature
    }

    staticAbility {
        effect = Effects.GrantKeyword(Keyword.VIGILANCE)
        filter = Filters.EquippedCreature
    }

    staticAbility {
        effect = Effects.GrantKeyword(Keyword.TRAMPLE)
        filter = Filters.EquippedCreature
    }

    staticAbility {
        effect = Effects.GrantKeyword(Keyword.HASTE)
        filter = Filters.EquippedCreature
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "395"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f21b5fc1-7611-44ac-ad8d-1f0c6d4fc9a3.jpg?1721428115"
        inBooster = false
    }
}
