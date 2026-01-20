package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.DiscardCardsEffect
import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Gristle Glutton
 *
 * {1}{R} Creature — Goblin Scout 1/3
 * {T}, Blight 1: Discard a card, then draw a card.
 */
object GristleGlutton {
    val definition = CardDefinition.creature(
        name = "Gristle Glutton",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype.GOBLIN, Subtype.SCOUT),
        power = 1,
        toughness = 3,
        oracleText = "{T}, Blight 1: Discard a card, then draw a card.",
        metadata = ScryfallMetadata(
            collectorNumber = "144",
            rarity = Rarity.COMMON,
            artist = "Filip Burburan",
            imageUri = "https://cards.scryfall.io/normal/front/c/c/cc4c4c4c-4c4c-4c4c-4c4c-4c4c4c4c4c4c.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Gristle Glutton") {
        // {T}, Blight 1: Discard a card, then draw a card (looting)
        activated(
            cost = AbilityCost.Composite(listOf(
                AbilityCost.Tap,
                AbilityCost.Blight(amount = 1)
            )),
            effect = DiscardCardsEffect(
                count = 1,
                target = EffectTarget.Controller
            ) then DrawCardsEffect(
                count = 1,
                target = EffectTarget.Controller
            )
        )
    }
}
