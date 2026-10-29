package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rise Again
 * {4}{B}
 * Sorcery
 * Return target creature card from your graveyard to the battlefield.
 */
val RiseAgain = card("Rise Again") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to the battlefield."

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Sean Sevestre"
        flavorText = "\"I love those who are eager to serve.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/660ec88f-2063-404a-853e-c985e21d17b0.jpg?1783930700"
    }
}
