package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Pattern Matcher — Core Set 2020 #234
 * {4} · Artifact Creature — Golem · 3/3
 *
 * When this creature enters, you may search your library for a card with the same name as
 * another creature you control, reveal it, put it into your hand, then shuffle.
 *
 * The search filter is `SharesNameWithYourCard` over creatures you control that aren't the source
 * (`notSourceItself`), so Pattern Matcher never matches its own name — but a second Pattern
 * Matcher can look at the first to find a third. The `Effects.May` gate wraps the whole search,
 * so declining skips the shuffle; accepting with no match is a legal "found nothing" search.
 */
val PatternMatcher = card("Pattern Matcher") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you may search your library for a card with the same name " +
        "as another creature you control, reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any.sharingNameWithPermanentYouControl(
                    GameObjectFilter.Creature.notSourceItself()
                ),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Jesper Ejsing"
        flavorText = "\"Good! Now put it with the other widdle bunnies.\"\n—Sargis Haz, artificer"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad608eb3-d387-4414-b194-703f38863896.jpg?1783932941"
        ruling("2019-07-12", "You may choose to search your library for nothing and shuffle it, even if you control no other creatures.")
        ruling("2019-07-12", "If a second Pattern Matcher enters the battlefield under your control, you can have it look at the first Pattern Matcher to search for a third Pattern Matcher.")
    }
}
