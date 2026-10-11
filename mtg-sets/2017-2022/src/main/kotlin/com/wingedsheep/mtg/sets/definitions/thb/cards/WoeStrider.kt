package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Woe Strider
 * {2}{B}
 * Creature — Horror
 * 3/2
 * When this creature enters, create a 0/1 white Goat creature token.
 * Sacrifice another creature: Scry 1.
 * Escape—{3}{B}{B}, Exile four other cards from your graveyard.
 * This creature escapes with two +1/+1 counters on it.
 *
 * "Escapes with two +1/+1 counters" (CR 702.138c) is an [EntersWithCounters] replacement gated on
 * [Conditions.Escaped], as on Ox of Agonas.
 */
val WoeStrider = card("Woe Strider") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, create a 0/1 white Goat creature token.\n" +
        "Sacrifice another creature: Scry 1.\n" +
        "Escape—{3}{B}{B}, Exile four other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)\n" +
        "This creature escapes with two +1/+1 counters on it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Goat"),
            imageUri = "https://cards.scryfall.io/normal/front/3/6/36cd5f96-5683-4959-b973-37f3c2fcf9bf.jpg?1783931456",
        )
        description = "When this creature enters, create a 0/1 white Goat creature token."
    }

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Patterns.Library.scry(1)
        description = "Sacrifice another creature: Scry 1."
    }

    keywordAbility(KeywordAbility.escape("{3}{B}{B}", Costs.additional.ExileOtherCards(4)))

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 2,
        selfOnly = true,
        condition = Conditions.Escaped
    ))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "John Thacker"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3457b558-b35b-49fd-b499-b1ec755f86ce.jpg?1783931556"

        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
        ruling(
            "2020-01-24",
            "If you cast a spell with its escape permission, you can't choose to apply any other alternative " +
                "costs or to cast it without paying its mana cost. If it has any additional costs, you must pay those."
        )
    }
}
