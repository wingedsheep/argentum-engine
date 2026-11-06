package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Renegade Tactics
 * {R}
 * Sorcery
 * Target creature can't block this turn.
 * Draw a card.
 */
val RenegadeTactics = card("Renegade Tactics") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature can't block this turn.\nDraw a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.CantBlock(t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Yeong-Hao Han"
        flavorText = "\"Nothing against you. It's just that waiting for Consulate clearance was never really my thing.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c06a39c-68bb-4e65-9a6d-9d9bc745201f.jpg?1783937189"
    }
}
