package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender

/**
 * Geist of the Lonely Vigil (Eldritch Moon #27)
 * {1}{W}
 * Creature — Spirit Cleric
 * 2/3
 *
 * Defender, flying
 * Delirium — This creature can attack as though it didn't have defender as long as there are four or
 * more card types among cards in your graveyard.
 *
 * `CanAttackDespiteDefender` gated on `Conditions.Delirium()`; the condition is only checked when
 * attackers are declared, so shrinking the graveyard afterwards doesn't remove it from combat.
 */
val GeistOfTheLonelyVigil = card("Geist of the Lonely Vigil") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Cleric"
    power = 2
    toughness = 3
    oracleText = "Defender, flying\n" +
        "Delirium — This creature can attack as though it didn't have defender as long as there are " +
        "four or more card types among cards in your graveyard."

    keywords(Keyword.DEFENDER, Keyword.FLYING)

    staticAbility {
        ability = CanAttackDespiteDefender(condition = Conditions.Delirium())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Ryan Yee"
        flavorText = "The flicker of candlelight and the echo of hymns told the villagers that the " +
            "monastery wasn't truly abandoned."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13118343-e026-4970-bf03-ba2d8fc58bac.jpg?1783937518"
        ruling(
            "2016-07-13",
            "Once you've attacked with Geist of the Lonely Vigil, lowering the number of card types in " +
                "your graveyard won't remove it from combat."
        )
    }
}
