package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tomb Robber
 * {2}{B}
 * Creature — Human Pirate
 * 1/1
 *
 * Menace
 * {1}, Discard a card: This creature explores.
 */
val TombRobber = card("Tomb Robber") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    power = 1
    toughness = 1
    oracleText = "Menace\n{1}, Discard a card: This creature explores. (Reveal the top card of your library. " +
        "Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, " +
        "then put the card back or put it into your graveyard.)"

    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.DiscardCard)
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Xi Zhang"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68132a28-b33b-48cb-acf8-42245312934e.jpg?1783935306"
    }
}
