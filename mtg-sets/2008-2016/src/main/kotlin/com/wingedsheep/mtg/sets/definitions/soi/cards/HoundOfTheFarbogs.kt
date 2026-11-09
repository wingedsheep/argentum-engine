package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Hound of the Farbogs (Shadows over Innistrad #117)
 * {4}{B}
 * Creature — Zombie Dog
 * 5/3
 *
 * Delirium — This creature has menace as long as there are four or more card types among cards in
 * your graveyard. (A creature with menace can't be blocked except by two or more creatures.)
 */
val HoundOfTheFarbogs = card("Hound of the Farbogs") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Dog"
    power = 5
    toughness = 3
    oracleText = "Delirium — This creature has menace as long as there are four or more card types among " +
        "cards in your graveyard. (A creature with menace can't be blocked except by two or more " +
        "creatures.)"

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Christine Choi"
        flavorText = "As you hear it baying in the distance, you'll feel its breath on your neck."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fc37615-ef72-4176-9e94-080e56af1d8a.jpg"
    }
}
