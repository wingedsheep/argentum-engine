package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crushing Disappointment — Strixhaven: School of Mages #68 (canonical printing)
 * {3}{B} · Instant
 *
 * Each player loses 2 life. You draw two cards.
 *
 * Every player — you included — loses the life ([Player.Each]); only you draw.
 */
val CrushingDisappointment = card("Crushing Disappointment") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Each player loses 2 life. You draw two cards."

    spell {
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.Each)) then
            Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Andrey Kuzinskiy"
        flavorText = "Most students could barely endure Dean Embrose's scrutiny for a semester or " +
            "two; Killian lived with it his entire life."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fbe8cf1-11df-4ca4-9a6b-265193c49250.jpg?1783927369"
    }
}
