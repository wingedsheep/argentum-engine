package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Charge Through — Strixhaven: School of Mages #124 (canonical printing)
 * {G} · Instant
 *
 * Target creature gains trample until end of turn.
 * Draw a card.
 *
 * Its only target going illegal makes the whole spell fizzle, so no card is drawn — per the ruling.
 */
val ChargeThrough = card("Charge Through") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gains trample until end of turn.\nDraw a card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Craig J Spearing"
        flavorText = "\"What a move! Lorehold gets caught flat-footed, and Witherbloom barrels ahead.\"\n—Cremik, Mage Tower commentator"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed16926f-feb0-481a-8c5e-88ea119752cd.jpg?1783927346"
        ruling("2021-04-16", "If the target becomes illegal before Charge Through resolves (perhaps because an opponent destroyed it), you won't draw a card.")
    }
}
