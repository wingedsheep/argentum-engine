package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Inquisitor's Ox (Shadows over Innistrad #24)
 * {3}{W}
 * Creature — Ox
 * 2/5
 *
 * Delirium — This creature gets +1/+0 and has vigilance as long as there are four or more card
 * types among cards in your graveyard.
 */
val InquisitorsOx = card("Inquisitor's Ox") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Ox"
    power = 2
    toughness = 5
    oracleText = "Delirium — This creature gets +1/+0 and has vigilance as long as there are four or more " +
        "card types among cards in your graveyard."

    staticAbility {
        ability = ModifyStats(1, 0, Filters.Self)
        condition = Conditions.Delirium()
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Vincent Proce"
        flavorText = "Many fields now lie fallow in Gavony, as the oxen have been confiscated to perform other " +
            "duties."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd1401fe-9e88-401b-96f0-5f58808a519f.jpg"
    }
}
