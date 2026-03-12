package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jousting Lance
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +2/+0.
 * During your turn, equipped creature has first strike.
 * Equip {3}
 */
val JoustingLance = card("Jousting Lance") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0.\nAs long as it's your turn, equipped creature has first strike.\nEquip {3}"

    staticAbility {
        effect = Effects.ModifyStats(+2, +0)
        filter = Filters.EquippedCreature
    }

    staticAbility {
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE)
        filter = Filters.EquippedCreature
        condition = Conditions.IsYourTurn
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "221"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/446579ab-5e36-44e2-84d7-cfa537619146.jpg?1562734807"
    }
}
