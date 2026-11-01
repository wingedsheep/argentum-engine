package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Persistent Petitioners
 * {1}{U}
 * Creature — Human Advisor
 * 1/3
 * {1}, {T}: Target player mills a card.
 * Tap four untapped Advisors you control: Target player mills twelve cards.
 * A deck can have any number of cards named Persistent Petitioners.
 *
 * The deck-construction clause is read from the oracle text by the server's DeckValidator,
 * like Rat Colony and Relentless Rats.
 */
val PersistentPetitioners = card("Persistent Petitioners") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Advisor"
    power = 1
    toughness = 3
    oracleText = "{1}, {T}: Target player mills a card. (They put the top card of their library into their graveyard.)\n" +
        "Tap four untapped Advisors you control: Target player mills twelve cards.\n" +
        "A deck can have any number of cards named Persistent Petitioners."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(1, player)
    }

    activatedAbility {
        cost = Costs.TapPermanents(
            count = 4,
            filter = GameObjectFilter.Creature.withSubtype(Subtype.ADVISOR),
        )
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(12, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e93f0c57-eb80-4dde-bdb0-326970491621.jpg?1783933707"
    }
}
