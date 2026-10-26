package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.DonorCards
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary

/**
 * Conspicuous Snoop
 * {R}{R}
 * Creature — Goblin Rogue
 * 2/2
 * Play with the top card of your library revealed.
 * You may cast Goblin spells from the top of your library.
 * As long as the top card of your library is a Goblin card, this creature has all activated
 * abilities of that card.
 *
 * The last line is the `TOP_OF_YOUR_LIBRARY` pool of [HasAllActivatedAbilitiesOfCards], narrowed
 * to Goblin cards: it re-reads the top card live, so the abilities change as the top card does.
 */
val ConspicuousSnoop = card("Conspicuous Snoop") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Rogue"
    power = 2
    toughness = 2
    oracleText = "Play with the top card of your library revealed.\n" +
        "You may cast Goblin spells from the top of your library.\n" +
        "As long as the top card of your library is a Goblin card, this creature has all activated abilities of that card."

    staticAbility { ability = RevealTopOfLibrary }

    staticAbility { ability = CastSpellTypesFromTopOfLibrary(GameObjectFilter.Any.withSubtype("Goblin")) }

    staticAbility {
        ability = HasAllActivatedAbilitiesOfCards(
            donors = DonorCards.TOP_OF_YOUR_LIBRARY,
            cardFilter = GameObjectFilter.Any.withSubtype("Goblin"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d878dab-5ed2-4ef3-b2c7-472290892854.jpg?1783930693"
        ruling("2020-06-23", "If the top card of your library changes while you're casting a spell, playing a land, or activating an ability, you don't reveal the new top card until you finish doing so. This means that if you cast the top card of your library, you can't look at the next one until you're done paying for that spell. While the top card isn't revealed, Conspicuous Snoop doesn't have any abilities of that card.")
        ruling("2020-06-23", "You must follow the normal timing permissions and restrictions of the Goblin spells you cast from your library.")
        ruling("2020-06-23", "You still pay all costs for a Goblin spell you cast from your library, including additional costs. You may also pay alternative costs.")
        ruling("2020-06-23", "If an activated ability of Goblin card on top of your library references the card it's printed on by name, treat Conspicuous Snoop's instance of that ability as though it referenced Conspicuous Snoop by name instead. For instance, if Conspicuous Snoop has Brash Taunter's ability, Conspicuous Snoop fights, not Brash Taunter.")
    }
}
