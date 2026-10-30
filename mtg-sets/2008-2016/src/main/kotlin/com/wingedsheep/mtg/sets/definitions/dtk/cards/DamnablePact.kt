package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Damnable Pact — Dragons of Tarkir #93 (canonical / earliest printing)
 * {X}{B}{B} · Sorcery
 *
 * Target player draws X cards and loses X life.
 *
 * Cut of the Profits' draw-then-lose pair, aimed at the target player instead of you.
 */
val DamnablePact = card("Damnable Pact") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player draws X cards and loses X life."

    spell {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(DynamicAmounts.xValue(), player) then
            Effects.LoseLife(DynamicAmounts.xValue(), player)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Zack Stella"
        flavorText = "\"Silumgar's mind is a dark labyrinth, full of grim secrets and subtle traps.\"\n—Siara, the Dragon's Mouth"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f65bcf62-48c3-4e4d-b129-8e03b3635326.jpg?1783938599"
    }
}
