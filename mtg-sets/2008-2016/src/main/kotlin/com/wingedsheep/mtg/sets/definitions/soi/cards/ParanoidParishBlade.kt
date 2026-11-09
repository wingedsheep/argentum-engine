package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Paranoid Parish-Blade (Shadows over Innistrad #33)
 * {2}{W}
 * Creature — Human Soldier
 * 3/2
 *
 * Delirium — This creature gets +1/+0 and has first strike as long as there are four or more card
 * types among cards in your graveyard.
 */
val ParanoidParishBlade = card("Paranoid Parish-Blade") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Delirium — This creature gets +1/+0 and has first strike as long as there are four or " +
        "more card types among cards in your graveyard."

    staticAbility {
        ability = ModifyStats(1, 0, Filters.Self)
        condition = Conditions.Delirium()
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Matt Stewart"
        flavorText = "The mind is perfectly capable of creating its own monsters."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16bb6c60-1889-49ed-8767-8144cc5a9571.jpg"
    }
}
