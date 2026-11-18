package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Yisan, the Wanderer Bard
 * {2}{G}
 * Legendary Creature — Human Rogue Bard
 * 2/3
 * {2}{G}, {T}, Put a verse counter on Yisan: Search your library for a creature card with mana
 * value equal to the number of verse counters on Yisan, put it onto the battlefield, then shuffle.
 *
 * The verse counter is an activation cost ([Costs.PutCounterOnSelf]), so it is already on Yisan
 * when the ability resolves: the first activation finds a mana value 1 creature. The count is read
 * on resolution through [DynamicAmounts.countersOnSelf], which falls back to Yisan's last-known
 * counters if it left the battlefield in response (the 2014-07-18 ruling).
 */
val YisanTheWandererBard = card("Yisan, the Wanderer Bard") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Rogue Bard"
    power = 2
    toughness = 3
    oracleText = "{2}{G}, {T}, Put a verse counter on Yisan: Search your library for a creature card " +
        "with mana value equal to the number of verse counters on Yisan, put it onto the battlefield, " +
        "then shuffle."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{G}"),
            Costs.Tap,
            Costs.PutCounterOnSelf(CounterType.VERSE),
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.withCardPredicate(
                CardPredicate.ManaValueEqualsDynamic(DynamicAmounts.countersOnSelf(CounterType.VERSE))
            ),
            destination = SearchDestination.BATTLEFIELD,
        )
        description = "{2}{G}, {T}, Put a verse counter on Yisan: Search your library for a creature card " +
            "with mana value equal to the number of verse counters on Yisan, put it onto the battlefield, " +
            "then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Chase Stone"
        flavorText = "Designed by Brian Fargo"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65cd97cd-6d6e-4512-a050-6851b7527567.jpg?1783939160"
        ruling(
            "2014-07-18",
            "If Yisan isn't on the battlefield when its ability resolves, use the number of verse counters " +
                "on it when it left the battlefield to determine which creature cards may be put onto the " +
                "battlefield. This number will include the counter you put on Yisan to activate the ability."
        )
    }
}
