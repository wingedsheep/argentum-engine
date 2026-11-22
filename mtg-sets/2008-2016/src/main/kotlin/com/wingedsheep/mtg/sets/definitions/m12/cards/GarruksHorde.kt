package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary

/**
 * Garruk's Horde — Magic 2012 #176 (canonical printing)
 * {5}{G}{G} · Creature — Beast · 7/7
 *
 * Trample
 * Play with the top card of your library revealed.
 * You may cast creature spells from the top of your library.
 *
 * The public reveal (`RevealTopOfLibrary`) and the creature-only cast permission
 * (`CastSpellTypesFromTopOfLibrary`) are separate statics: neither alone is the card.
 */
val GarruksHorde = card("Garruk's Horde") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 7
    toughness = 7
    oracleText = "Trample\n" +
        "Play with the top card of your library revealed.\n" +
        "You may cast creature spells from the top of your library. (Do this only any time you could cast that creature spell. You still pay the spell's costs.)"

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = RevealTopOfLibrary
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Creature
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/563c6959-9131-40a6-97ec-12baf6fb7ca0.jpg?1783941059"
        ruling("2013-04-15", "The top card of your library isn't in your hand, so you can't suspend it, cycle it, discard it, or activate any of its activated abilities.")
        ruling("2013-04-15", "If the top card of your library changes while you're casting a spell, playing a land, or activating an ability, the new top card won't be revealed until you finish doing so.")
        ruling("2011-09-22", "Normally, Garruk's Horde allows you to cast the top card of your library if it's a creature card, it's your main phase, and the stack is empty. If that creature card has flash, you'll be able to cast it at the time you could cast an instant, even on an opponent's turn.")
        ruling("2011-09-22", "You'll still pay all costs for that spell, including additional costs. You may also pay alternative costs.")
        ruling("2011-09-22", "If the top card of your library is Dryad Arbor (the only card that's both a creature and a land), you can't play it this way. Dryad Arbor can't be cast as a spell.")
    }
}
