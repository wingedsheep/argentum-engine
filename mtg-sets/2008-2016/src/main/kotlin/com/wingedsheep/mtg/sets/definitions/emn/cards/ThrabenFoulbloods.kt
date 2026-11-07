package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Thraben Foulbloods (Eldritch Moon #108)
 * {2}{B}
 * Creature — Zombie Dog
 * 3/2
 *
 * Delirium — This creature gets +1/+1 and has menace as long as there are four or more card types
 * among cards in your graveyard.
 */
val ThrabenFoulbloods = card("Thraben Foulbloods") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Dog"
    power = 3
    toughness = 2
    oracleText = "Delirium — This creature gets +1/+1 and has menace as long as there are four or " +
        "more card types among cards in your graveyard. (A creature with menace can't be blocked " +
        "except by two or more creatures.)"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.Self)
        condition = Conditions.Delirium()
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Deruchenko Alexander"
        flavorText = "The famed hunting hounds of House Cecani continue to prove the quality of " +
            "their pedigree."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf0f72e0-1254-4bfb-b405-d64012dc171a.jpg"
    }
}
