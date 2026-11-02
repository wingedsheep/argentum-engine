package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Ajani's Influence — {2}{W}{W} Sorcery
 *
 * Put two +1/+1 counters on target creature.
 * Look at the top five cards of your library. You may reveal a white card from among them and put it
 * into your hand. Put the rest on the bottom of your library in a random order.
 */
val AjanisInfluence = card("Ajani's Influence") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put two +1/+1 counters on target creature.\n" +
        "Look at the top five cards of your library. You may reveal a white card from among them and put it " +
        "into your hand. Put the rest on the bottom of your library in a random order."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature) then
            Patterns.Library.lookAtTopRevealMatchingToHand(
                count = 5,
                filter = GameObjectFilter.Any.withCardPredicate(CardPredicate.HasColor(Color.WHITE)),
                prompt = "You may reveal a white card from among them and put it into your hand",
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "282"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d38ad178-35a5-44ad-b80a-d746f3e6a45c.jpg?1783934491"
    }
}
