package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapLimitPerStep

// Modern Oracle explicitly makes the restriction conditional on this artifact being untapped.
val WinterOrb = card("Winter Orb") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "As long as this artifact is untapped, players can't untap more than one land during their untap steps."

    staticAbility {
        condition = Conditions.SourceIsUntapped
        ability = UntapLimitPerStep(GameObjectFilter.Land, 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "275"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9359f60c-9a27-4e53-b35b-964a121a6fba.jpg?1783948660"
        ruling("2016-06-08", "All permanents untap during a player’s untap step at once. If Winter Orb is tapped as your untap step begins, your lands will all untap.")
    }
}
