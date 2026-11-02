package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mausoleum Secrets
 * {1}{B}
 * Instant
 * Undergrowth — Search your library for a black card with mana value less than or equal to the
 * number of creature cards in your graveyard, reveal it, put it into your hand, then shuffle.
 *
 * The graveyard count is read by the dynamic mana-value predicate when the search gathers its
 * candidates, i.e. on resolution.
 */
val MausoleumSecrets = card("Mausoleum Secrets") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Undergrowth — Search your library for a black card with mana value less than or equal to " +
        "the number of creature cards in your graveyard, reveal it, put it into your hand, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withColor(Color.BLACK)
                .manaValueAtMostDynamic(DynamicAmounts.creatureCardsInYourGraveyard()),
            count = 1,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26f7cf38-78cc-4139-9f2a-4dd0be7d9da8.jpg?1783934174"
        ruling("2018-10-05", "A land card that produces black mana, even a Swamp, normally has no color.")
        ruling("2018-10-05", "If a card in your library has {X} in its mana cost, X is considered to be 0.")
        ruling("2018-10-05", "Creature cards with other types, such as artifact creature cards, count for undergrowth abilities.")
        ruling("2018-10-05", "Because tokens aren't cards, they never count for undergrowth abilities.")
    }
}
