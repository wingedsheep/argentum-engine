package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Costs
/**
 * Ruthless Ripper
 * {B}
 * Creature — Human Assassin
 * 1/1
 * Deathtouch
 * Morph—Reveal a black card in your hand.
 * When this creature is turned face up, target player loses 2 life.
 */
val RuthlessRipper = card("Ruthless Ripper") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    power = 1
    toughness = 1
    oracleText = "Deathtouch\nMorph—Reveal a black card in your hand. (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, target player loses 2 life."

    keywords(Keyword.DEATHTOUCH)

    morphCost = Costs.pay.RevealCard(filter = GameObjectFilter.Any.withColor(Color.BLACK))

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val player = target("target player", Targets.Player)
        effect = Effects.LoseLife(2, player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9c67b11-a82e-4a25-88a4-5771ab5c4f00.jpg?1562793470"
    }
}
