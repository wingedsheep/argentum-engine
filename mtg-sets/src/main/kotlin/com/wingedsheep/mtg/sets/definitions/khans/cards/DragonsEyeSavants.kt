package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect

/**
 * Dragon's Eye Savants
 * {1}{U}
 * Creature — Human Wizard
 * 0/6
 * Morph—Reveal a blue card in your hand.
 * When this creature is turned face up, look at target opponent's hand.
 */
val DragonsEyeSavants = card("Dragon's Eye Savants") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Human Wizard"
    power = 0
    toughness = 6
    oracleText = "Morph—Reveal a blue card in your hand. (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, look at target opponent's hand."

    morphCost = PayCost.RevealCard(filter = GameObjectFilter.Any.withColor(Color.BLUE))

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("target opponent", Targets.Opponent)
        effect = LookAtTargetHandEffect(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9ff29b2c-2156-45fd-a2dd-655b8d208197.jpg?1562791201"
    }
}
