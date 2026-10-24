package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Specter of the Fens — Strixhaven: School of Mages #87 (canonical printing)
 * {3}{B} · Creature — Specter · 2/3
 *
 * Flying
 * {5}{B}: Target opponent loses 2 life and you gain 2 life.
 */
val SpecterOfTheFens = card("Specter of the Fens") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Specter"
    power = 2
    toughness = 3
    oracleText = "Flying\n{5}{B}: Target opponent loses 2 life and you gain 2 life."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{5}{B}")
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(2, opponent) then Effects.GainLife(2)
        description = "{5}{B}: Target opponent loses 2 life and you gain 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "G-host Lee"
        flavorText = "Foul specters hunt down those who try to back out of a deal with a daemogoth."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73e3cfbc-7177-4bbc-89c4-560c9c2f97db.jpg?1783927361"
    }
}
