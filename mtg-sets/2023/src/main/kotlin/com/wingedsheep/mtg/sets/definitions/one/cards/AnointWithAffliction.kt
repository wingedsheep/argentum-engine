package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Anoint with Affliction
 * {1}{B}
 * Instant
 * Exile target creature if it has mana value 3 or less.
 * Corrupted — Exile that creature instead if its controller has three or more poison counters.
 *
 * Both clauses are checked on resolution and end in the same exile, so the card collapses to one
 * gate: exile if the target's mana value is 3 or less **or** its controller has three or more
 * poison counters. The corrupted clause reads the *target creature's controller*, not "an
 * opponent", so it is [Conditions.PoisonCountersAtLeast] over `Player.ControllerOf(...)`.
 */
val AnointWithAffliction = card("Anoint with Affliction") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target creature if it has mana value 3 or less.\n" +
        "Corrupted — Exile that creature instead if its controller has three or more poison counters."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.Any(
                Conditions.TargetMatchesFilter(GameObjectFilter.Any.manaValueAtMost(3), creature),
                Conditions.PoisonCountersAtLeast(3, Player.ControllerOf("target creature")),
            ),
            then = Effects.Exile(creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "David Astruga"
        flavorText = "Many vatkeepers use necrogen etchings to judge the value of their aspirants. If the " +
            "wounds rot and fester, the recipient is not worth further effort."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7dc55a8-290f-4666-90ce-aa632e87c5e7.jpg?1783918052"
    }
}
