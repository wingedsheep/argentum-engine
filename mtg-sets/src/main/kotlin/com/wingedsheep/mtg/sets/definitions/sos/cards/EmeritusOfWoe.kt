package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker

/**
 * Emeritus of Woe // Demonic Tutor — Secrets of Strixhaven #80
 * {3}{B} · Creature — Vampire Warlock · 5/4
 *
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so
 * unprepares it.)
 * At the beginning of your end step, if two or more creatures died this turn, this creature
 * becomes prepared.
 * //
 * Demonic Tutor — {1}{B}, Sorcery: Search your library for a card, put that card into your hand,
 * then shuffle.
 *
 * Prepare (Secrets of Strixhaven): enters with the PREPARED keyword. The end-step ability
 * re-prepares it via [Effects.BecomePrepared], gated as an intervening-if (CR 603.4) on two or
 * more creatures having died this turn — a global count across all players, expressed as
 * `Compare(TurnTracking(Each, CREATURES_DIED) GTE 2)`. Becoming prepared creates a copy of its
 * prepare spell ("Demonic Tutor") in exile that its controller may cast for {1}{B}; casting that
 * copy unprepares the creature. Modeled via [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the
 * `prepare(name) { }` DSL.
 */
val EmeritusOfWoe = card("Emeritus of Woe") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Warlock"
    power = 5
    toughness = 4
    oracleText = "This creature enters prepared. (While it's prepared, you may cast a copy of its " +
        "spell. Doing so unprepares it.)\n" +
        "At the beginning of your end step, if two or more creatures died this turn, this creature " +
        "becomes prepared."

    keywords(Keyword.PREPARED)

    // At the beginning of your end step, if two or more creatures died this turn, it becomes prepared.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmount.TurnTracking(Player.Each, TurnTracker.CREATURES_DIED),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(2),
        )
        effect = Effects.BecomePrepared(EffectTarget.Self)
    }

    // Demonic Tutor — the prepare spell. Search your library for a card, to hand, then shuffle.
    prepare("Demonic Tutor") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery"
        oracleText = "Search your library for a card, put that card into your hand, then shuffle."
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any,
                destination = SearchDestination.HAND,
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "80"
        artist = "Jodie Muir"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7eb9e83d-515d-4911-a06b-9982200277b2.jpg?1778165065"
    }
}
