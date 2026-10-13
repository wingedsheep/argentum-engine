package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lagomos, Hand of Hatred — Dominaria United #205
 * {1}{B}{R} · Legendary Creature — Human Shaman · 1/3
 *
 * At the beginning of combat on your turn, create a 2/1 red Elemental creature token with trample
 * and haste. Sacrifice it at the beginning of the next end step.
 * {T}: Search your library for a card, put it into your hand, then shuffle. Activate only if five
 * or more creatures died this turn.
 *
 * The token is the Harried Dronesmith shape: [Effects.CreateToken] with `sacrificeAtStep = Step.END`
 * arms a delayed sacrifice of that specific token at the next end step.
 *
 * "Five or more creatures died this turn" counts every creature, tokens included, under anyone's
 * control — `creaturesDiedThisTurn(Player.Each)` sums every seat's `CREATURES_DIED` tracker.
 */
val LagomosHandOfHatred = card("Lagomos, Hand of Hatred") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Shaman"
    power = 1
    toughness = 3
    oracleText = "At the beginning of combat on your turn, create a 2/1 red Elemental creature " +
        "token with trample and haste. Sacrifice it at the beginning of the next end step.\n" +
        "{T}: Search your library for a card, put it into your hand, then shuffle. Activate only " +
        "if five or more creatures died this turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Elemental"),
            keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
            sacrificeAtStep = Step.END,
            imageUri = "https://cards.scryfall.io/normal/front/c/6/c6b2ff6f-d55a-4fa2-86be-e2e012267de4.jpg?1783921129",
        )
        description = "At the beginning of combat on your turn, create a 2/1 red Elemental " +
            "creature token with trample and haste. Sacrifice it at the beginning of the next end step."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any,
            destination = SearchDestination.HAND,
        )
        description = "Search your library for a card, put it into your hand, then shuffle. " +
            "Activate only if five or more creatures died this turn."
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    DynamicAmounts.creaturesDiedThisTurn(Player.Each),
                    ComparisonOperator.GTE,
                    5,
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a6bb0c2-1e3c-48cd-8f10-49f047e0514c.jpg?1783921281"
    }
}
