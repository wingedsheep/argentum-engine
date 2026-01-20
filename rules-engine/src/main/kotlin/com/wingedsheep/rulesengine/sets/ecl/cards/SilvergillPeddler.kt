package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CompositeEffect
import com.wingedsheep.rulesengine.ability.DiscardCardsEffect
import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnBecomesTapped
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Silvergill Peddler
 *
 * {2}{U} Creature — Merfolk Citizen 2/3
 * Whenever this creature becomes tapped, draw a card, then discard a card.
 */
object SilvergillPeddler {
    val definition = CardDefinition.creature(
        name = "Silvergill Peddler",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype.MERFOLK, Subtype.CITIZEN),
        power = 2,
        toughness = 3,
        oracleText = "Whenever this creature becomes tapped, draw a card, then discard a card.",
        metadata = ScryfallMetadata(
            collectorNumber = "70",
            rarity = Rarity.COMMON,
            artist = "John Tedrick",
            imageUri = "https://cards.scryfall.io/normal/front/c/c/cc3c3c3c-3c3c-3c3c-3c3c-3c3c3c3c3c3c.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Silvergill Peddler") {
        // Whenever tapped, loot (draw then discard)
        triggered(
            trigger = OnBecomesTapped(selfOnly = true),
            effect = CompositeEffect(
                effects = listOf(
                    DrawCardsEffect(count = 1, target = EffectTarget.Controller),
                    DiscardCardsEffect(count = 1, target = EffectTarget.Controller)
                )
            )
        )
    }
}
