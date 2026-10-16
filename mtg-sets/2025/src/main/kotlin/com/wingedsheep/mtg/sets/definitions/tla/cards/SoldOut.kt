package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sold Out
 * {3}{B}
 * Instant
 *
 * Exile target creature. If it was dealt damage this turn, create a Clue token.
 * (It's an artifact with "{2}, Sacrifice this token: Draw a card.")
 *
 * The "if it was dealt damage this turn" clause is evaluated while the creature is
 * still on the battlefield — exiling it would clear its damage tracking — so the Clue
 * branch runs first, then the exile. Both happen in the same resolution, so the order
 * is observationally identical to the printed text.
 */
val SoldOut = card("Sold Out") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target creature. If it was dealt damage this turn, create a Clue token. " +
        "(It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.copy(
                    statePredicates = listOf(StatePredicate.WasDealtDamageThisTurn()),
                ), creature),
            then = Effects.CreateClue(),
        ) then Effects.Exile(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Nijihayashi"
        flavorText = "Haru's good deed went severely punished."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1affba6d-2cca-4cdf-8690-1e23ffbe8462.jpg?1764120823"
    }
}
