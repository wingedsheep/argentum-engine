package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Jushi Apprentice // Tomoya the Revealer (Champions of Kamigawa #70) — a flip card (CR 710).
 *
 * Jushi Apprentice {1}{U} — Creature — Human Wizard 1/2
 * "{2}{U}, {T}: Draw a card. If you have nine or more cards in hand, flip this creature."
 *
 * Tomoya the Revealer — Legendary Creature — Human Wizard 2/3
 * "{3}{U}{U}, {T}: Target player draws X cards, where X is the number of cards in your hand."
 *
 * The hand-size check reads the hand *after* the draw — it's the rider's own clause, evaluated
 * as the rider resolves. X is counted as Tomoya's ability resolves, so a player targeting
 * themselves draws as many cards as they held just before drawing.
 */
private val JushiApprenticeUpright = card("Jushi Apprentice") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "{2}{U}, {T}: Draw a card. If you have nine or more cards in hand, flip this creature."
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        effect = Effects.DrawCards(1) then
            Effects.If(Conditions.CardsInHandAtLeast(9), Effects.Flip())
        description = "{2}{U}, {T}: Draw a card. If you have nine or more cards in hand, flip this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33a8e5b9-6bfb-4ff2-a16d-3168a5412807.jpg?1783944325"
    }
}

private val TomoyaTheRevealer = card("Tomoya the Revealer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    oracleText = "{3}{U}{U}, {T}: Target player draws X cards, where X is the number of cards in your hand."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{U}{U}"), Costs.Tap)
        val player = target(Targets.Player)
        effect = Effects.DrawCards(DynamicAmounts.cardsInYourHand(), player)
        description = "{3}{U}{U}, {T}: Target player draws X cards, where X is the number of cards in your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33a8e5b9-6bfb-4ff2-a16d-3168a5412807.jpg?1783944325"
    }
}

val JushiApprentice: CardDefinition = CardDefinition.flipCard(
    unflipped = JushiApprenticeUpright,
    flipped = TomoyaTheRevealer,
)
