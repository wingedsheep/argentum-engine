package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.model.Rarity

/**
 * Stingerback Terror
 * {2}{R}{R}
 * Creature — Scorpion Dragon
 * 7/7
 *
 * Flying, trample
 * This creature gets -1/-1 for each card in your hand.
 * Plot {2}{R}
 */
val StingerbackTerror = card("Stingerback Terror") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Scorpion Dragon"
    power = 7
    toughness = 7
    oracleText = "Flying, trample\n" +
        "This creature gets -1/-1 for each card in your hand.\n" +
        "Plot {2}{R} (You may pay {2}{R} and exile this card from your hand. Cast it as a sorcery " +
        "on a later turn without paying its mana cost. Plot only as a sorcery.)"

    keywords(Keyword.FLYING, Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.plot("{2}{R}"))

    staticAbility {
        // -1/-1 for each card in your hand, applied to this creature itself.
        val negHand = DynamicAmount.Multiply(DynamicAmounts.cardsInYourHand(), -1)
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = negHand,
            toughnessBonus = negHand
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d84d6e52-5c35-47bc-b160-876a3b0fcbe1.jpg?1712355854"
    }
}
