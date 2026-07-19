package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Leyline Axe
 * {4}
 * Artifact — Equipment
 *
 * If this card is in your opening hand, you may begin the game with it on the battlefield.
 * Equipped creature gets +1/+1 and has double strike and trample.
 * Equip {3}
 *
 * The opening-hand clause is the shared [mayBeginGameOnBattlefield] helper — despite the card's
 * name, it's an Equipment, not a "Leyline of X" enchantment; the ability is the reusable one.
 */
val LeylineAxe = card("Leyline Axe") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield.\n" +
        "Equipped creature gets +1/+1 and has double strike and trample.\n" +
        "Equip {3}"

    mayBeginGameOnBattlefield()

    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Edgar Sánchez Hidalgo"
        flavorText = "It awaits a worthy wielder."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9c03336-a321-4c06-94d1-809f328fabd8.jpg?1783909089"
    }
}
