package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Tale's End
 * {1}{U}
 * Instant
 * Counter target activated ability, triggered ability, or legendary spell.
 *
 * An ability on the stack has no card characteristics, so `IsLegendary` only ever matches a spell,
 * and `IsActivatedOrTriggeredAbility` only ever matches an ability — the `Or` reads exactly as
 * printed. Mana abilities never use the stack, so they can't be targeted.
 */
val TalesEnd = card("Tale's End") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target activated ability, triggered ability, or legendary spell."

    spell {
        target(
            TargetFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(
                            listOf(CardPredicate.IsActivatedOrTriggeredAbility, CardPredicate.IsLegendary)
                        )
                    )
                ),
                zone = Zone.STACK
            )
        )
        effect = Effects.CounterSpellOrAbility()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Randy Vargas"
        flavorText = "\"When you are gone, will anyone remember your story?\"\n—Unknown"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/1421115b-9a98-4ab2-bcb2-7d8899ce12db.jpg?1783933002"
        ruling(
            "2019-07-12",
            "Tale's End can't target an activated or triggered mana ability. An activated mana ability is one " +
                "that could add mana as it resolves, doesn't have a target, and isn't a loyalty ability. A " +
                "triggered mana ability is one that could add mana as it resolves and triggers on an activated " +
                "mana ability."
        )
        ruling(
            "2019-07-12",
            "Abilities that create replacement effects, such as a permanent entering the battlefield tapped or " +
                "with counters on it, can't be targeted. Abilities that apply \"as [this creature] enters the " +
                "battlefield\" are also replacement effects and can't be targeted."
        )
        ruling(
            "2019-07-12",
            "If you counter a delayed triggered ability that triggered at the beginning of the \"next\" " +
                "occurrence of a specified step or phase, that ability won't trigger again the following time " +
                "that step or phase occurs."
        )
    }
}
