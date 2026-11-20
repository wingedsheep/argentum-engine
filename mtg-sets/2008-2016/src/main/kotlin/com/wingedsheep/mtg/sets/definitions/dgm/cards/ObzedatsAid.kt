package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Obzedat's Aid
 * {3}{W}{B}
 * Sorcery
 * Return target permanent card from your graveyard to the battlefield.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val ObzedatsAid = card("Obzedat's Aid") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Return target permanent card from your graveyard to the battlefield."

    spell {
        val t = target(TargetFilter.PermanentInYourGraveyard)
        effect = Effects.Move(t, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Dan Murayama Scott"
        flavorText = "\"The Obzedat have revived you with purpose. Don't squander their blessing.\"\n—Teysa Karlov, Grand Envoy of Orzhov"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b846ba99-81ba-424a-98eb-f9f69c40f984.jpg"
    }
}
