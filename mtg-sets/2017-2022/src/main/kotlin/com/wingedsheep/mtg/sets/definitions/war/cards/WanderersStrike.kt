package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wanderer's Strike
 * {4}{W}
 * Sorcery
 * Exile target creature, then proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)
 */
val WanderersStrike = card("Wanderer's Strike") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target creature, then proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Exile(creature) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Sara Winters"
        flavorText = "\"Precise, clean cuts. No one saw anything. Again. Tag the report with my code and file it.\"\n—Lavinia"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85e1d63e-9692-4988-bda2-7c2c9b42217b.jpg?1783933471"
    }
}
