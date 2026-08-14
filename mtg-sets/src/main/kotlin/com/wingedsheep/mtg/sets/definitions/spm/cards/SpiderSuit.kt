package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Spider-Suit
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +2/+2 and is a Spider Hero in addition to its other types.
 * Equip {3}
 */
val SpiderSuit = card("Spider-Suit") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2 and is a Spider Hero in addition to its other types.\nEquip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantSubtype("Spider", Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantSubtype("Hero", Filters.EquippedCreature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Alex Horley-Orlandelli"
        flavorText = "Nothing beats the classic look."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/436527ec-5af4-4b6d-a5a0-d21fc466a625.jpg?1757378101"
    }
}
