package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Tempest Hawk — Tarkir: Dragonstorm #31
 * {2}{W} · Creature — Bird · 2/2
 *
 * Flying
 * Whenever this creature deals combat damage to a player, you may search your library for a card
 * named Tempest Hawk, reveal it, put it into your hand, then shuffle.
 * A deck can have any number of cards named Tempest Hawk.
 *
 * The combat-damage trigger reuses [Triggers.DealsCombatDamageToPlayer] + the
 * `LibraryPatterns.searchLibrary` Gather→Select→Move pipeline, filtered to the card's own name via
 * `CardPredicate.NameEquals`. The search is `ChooseUpTo(1)`, so selecting zero cards is the "you may"
 * decline (no separate yes/no needed) — the same idiom as Embermouth Sentinel's optional ETB search.
 *
 * The final line ("A deck can have any number...") is a deck-construction allowance (CR 100.4-style
 * exemption from the four-copy rule). The engine does not enforce singleton/four-copy deck limits at
 * gameplay time, so this clause has no in-game behavior to model; it is preserved in [oracleText] only.
 */
val TempestHawk = card("Tempest Hawk") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, you may search your library for a " +
        "card named Tempest Hawk, reveal it, put it into your hand, then shuffle.\n" +
        "A deck can have any number of cards named Tempest Hawk."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter(cardPredicates = listOf(CardPredicate.NameEquals("Tempest Hawk"))),
            count = 1,
            destination = SearchDestination.HAND,
            shuffleAfter = true,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Abz J Harding"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/422f9453-ab12-4e3c-8c51-be87391395a1.jpg?1743204081"
    }
}
