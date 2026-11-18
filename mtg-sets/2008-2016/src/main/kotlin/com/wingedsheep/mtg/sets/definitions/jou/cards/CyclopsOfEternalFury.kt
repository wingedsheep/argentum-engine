package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Cyclops of Eternal Fury
 * {4}{R}{R}
 * Enchantment Creature — Cyclops
 * 5/3
 *
 * Creatures you control have haste.
 *
 * Not "other": the Cyclops grants itself haste too, so it can attack the turn it enters.
 */
val CyclopsOfEternalFury = card("Cyclops of Eternal Fury") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Cyclops"
    power = 5
    toughness = 3
    oracleText = "Creatures you control have haste."

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Matt Stewart"
        flavorText = "\"The anger that festers inside this one is contagious.\"\n—Anthousa of Setessa"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44a92b7a-7119-45b3-b9ed-5dbf5abca818.jpg?1783939427"
    }
}
