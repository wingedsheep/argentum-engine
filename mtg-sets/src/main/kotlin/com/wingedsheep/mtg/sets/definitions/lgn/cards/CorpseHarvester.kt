package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Corpse Harvester
 * {3}{B}{B}
 * Creature — Zombie Wizard
 * 3/3
 * {1}{B}, {T}, Sacrifice a creature: Search your library for a Zombie card and a Swamp card,
 * reveal them, put them into your hand, then shuffle.
 */
val CorpseHarvester = card("Corpse Harvester") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Wizard"
    power = 3
    toughness = 3
    oracleText = "{1}{B}, {T}, Sacrifice a creature: Search your library for a Zombie card and a Swamp card, reveal them, put them into your hand, then shuffle."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature)
        )
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter(
                cardPredicates = listOf(CardPredicate.HasSubtype(Subtype("Zombie")))
            ),
            reveal = true,
            shuffleAfter = false
        ) then LibraryPatterns.searchLibrary(
            filter = GameObjectFilter(
                cardPredicates = listOf(CardPredicate.HasSubtype(Subtype("Swamp")))
            ),
            reveal = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d09c2c8-526b-4693-bbaa-109911ce5281.jpg?1562897662"
        ruling("2004-10-04", "You do not have to find a Zombie card or swamp card if you do not want to, even if you have them in your library.")
    }
}
