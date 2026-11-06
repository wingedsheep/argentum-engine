package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Grim Strider
 * {3}{B}
 * Creature — Horror
 * 6/6
 * This creature gets -1/-1 for each card in your hand.
 *
 * The shrink is Stingerback Terror's self-scoped [GrantDynamicStats] over your hand size.
 */
val GrimStrider = card("Grim Strider") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 6
    toughness = 6
    oracleText = "This creature gets -1/-1 for each card in your hand."

    staticAbility {
        val negHand = -DynamicAmounts.cardsInYourHand()
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = negHand,
            toughnessBonus = negHand
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Christine Choi"
        flavorText = "\"As a child I saw something walking in the distance, far beyond the Hekma—shadow-dark, " +
            "and taller than the gods. Even now, it passes through my dreams.\"\n—Pytamun, initiate of Nef crop"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97ae6769-2b2b-48e3-9503-e9744984743a.jpg?1783936506"
    }
}
