package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Phantom Carriage (Innistrad: Midnight Hunt #70)
 * {4}{U}{U} · Creature — Spirit 4/4
 * Flying
 * When this creature enters, you may search your library for a card with flashback or disturb,
 * put it into your graveyard, then shuffle.
 *
 * "A card with flashback or disturb" reads the FLASHBACK / DISTURB keywords, which the card
 * builder derives from each card's `flashback(...)` / `disturb(...)` keyword ability.
 */
val PhantomCarriage = card("Phantom Carriage") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, you may search your library for a card with flashback or disturb, " +
        "put it into your graveyard, then shuffle."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.HasKeyword(Keyword.FLASHBACK),
                            CardPredicate.HasKeyword(Keyword.DISTURB)
                        )
                    )
                )
            ),
            destination = SearchDestination.GRAVEYARD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Igor Kieryluk"
        flavorText = "Passage to the abyss is free, but the return trip will cost you."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/1990ed41-3739-40f6-8797-359f88af05f9.jpg?1783925633"
    }
}
