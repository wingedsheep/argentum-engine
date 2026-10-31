package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Mysterious Pathlighter — Throne of Eldraine #22
 * {2}{W} · Creature — Faerie · 2/2
 *
 * Flying
 * Each creature you control that has an Adventure enters with an additional +1/+1 counter on it.
 *
 * Bramblewood Paragon's [EntersWithCounters] replacement, with the group narrowed by
 * [CardPredicate.HasAdventure] — a layout characteristic of the whole card, so it matches an
 * adventurer creature whether it was cast from hand or from exile after its Adventure.
 */
val MysteriousPathlighter = card("Mysterious Pathlighter") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Faerie"
    power = 2
    toughness = 2
    oracleText = "Flying\nEach creature you control that has an Adventure enters with an additional +1/+1 " +
        "counter on it. (It doesn't need to have gone on the adventure first.)"

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.withCardPredicate(CardPredicate.HasAdventure).youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Kimonas Theodossiou"
        flavorText = "For good or ill, most fae can't resist meddling in the adventures of any knight they meet."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1028aa10-aded-4aeb-b70f-d7a9003ae846.jpg?1783932669"
        ruling(
            "2019-10-04",
            "A creature with an Adventure you control that would enter the battlefield with no +1/+1 " +
                "counters on it enters with one +1/+1 counter on it."
        )
        ruling(
            "2019-10-04",
            "An effect may refer to a card, spell, or permanent that \"has an Adventure.\" This refers to " +
                "a card, spell, or permanent that has an adventurer card's set of alternative " +
                "characteristics, even if they're not being used and even if that card was never cast as an Adventure."
        )
        ruling(
            "2019-10-04",
            "If an effect refers to a card, spell, or permanent that has an Adventure, it won't find an " +
                "instant or sorcery spell on the stack that's been cast as an Adventure."
        )
    }
}
