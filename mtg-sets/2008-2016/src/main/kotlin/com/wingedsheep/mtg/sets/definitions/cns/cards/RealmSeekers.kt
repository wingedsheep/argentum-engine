package com.wingedsheep.mtg.sets.definitions.cns.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Realm Seekers
 * {4}{G}{G}
 * Creature — Elf Scout
 * 0/0
 * This creature enters with X +1/+1 counters on it, where X is the total number of cards in all
 * players' hands.
 * {2}{G}, Remove a +1/+1 counter from this creature: Search your library for a land card, reveal it,
 * put it into your hand, then shuffle.
 *
 * X is counted as the creature enters (a replacement, not a trigger). Cast normally it is on the
 * stack, so it doesn't count itself; put onto the battlefield straight from a hand, the entry
 * replacement reads the pre-entry zones, so it does — the first ruling.
 */
val RealmSeekers = card("Realm Seekers") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 0
    toughness = 0
    oracleText = "This creature enters with X +1/+1 counters on it, where X is the total number of " +
        "cards in all players' hands.\n" +
        "{2}{G}, Remove a +1/+1 counter from this creature: Search your library for a land card, " +
        "reveal it, put it into your hand, then shuffle."

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = DynamicAmounts.count(Player.Each, Zone.HAND)
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{G}"),
            Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 1)
        )
        effect = Patterns.Library.searchLibrary(
            filter = Filters.Land,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Mike Sass"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e469f84-004d-4a92-98fe-9869b4945181.jpg?1783939373"
        ruling("2014-05-29", "If Realm Seekers enters the battlefield directly from a player's hand without being cast, it counts itself when determining the value of X.")
        ruling("2014-05-29", "The value of X is determined as Realm Seekers enters the battlefield. Players can cast spells and activate abilities while Realm Seekers is on the stack to remove cards from their hands if they wish, and this will result in fewer counters being placed on Realm Seekers as it enters the battlefield.")
    }
}
