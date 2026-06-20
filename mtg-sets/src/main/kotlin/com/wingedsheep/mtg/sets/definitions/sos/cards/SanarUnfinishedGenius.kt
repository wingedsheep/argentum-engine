package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Sanar, Unfinished Genius // Wild Idea — Secrets of Strixhaven #223
 * {U}{R} · Legendary Creature — Goblin Sorcerer · 0/4
 *
 * Sanar enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so
 * unprepares it.)
 * {T}: Create a Treasure token. Activate only if you've cast an instant or sorcery spell this turn.
 * //
 * Wild Idea — {3}{U}{R}, Sorcery: Search your library for an instant or sorcery card, reveal it,
 * put it into your hand, then shuffle.
 *
 * Prepare (Secrets of Strixhaven): the [Keyword.PREPARED] keyword makes Sanar enter prepared,
 * creating a castable copy of "Wild Idea" in exile. The Treasure ability is a vanilla
 * [Costs.Tap] activation gated by an [ActivationRestriction.OnlyIfCondition] on
 * [Conditions.YouCastSpellsThisTurn] for instant/sorcery spells.
 */
val SanarUnfinishedGenius = card("Sanar, Unfinished Genius") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Goblin Sorcerer"
    power = 0
    toughness = 4
    oracleText = "Sanar enters prepared. (While it's prepared, you may cast a copy of its spell. " +
        "Doing so unprepares it.)\n{T}: Create a Treasure token. Activate only if you've cast an " +
        "instant or sorcery spell this turn."

    keywords(Keyword.PREPARED)

    // {T}: Create a Treasure token. Only if you've cast an instant or sorcery this turn.
    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouCastSpellsThisTurn(1, GameObjectFilter.InstantOrSorcery),
            ),
        )
        effect = Effects.CreateTreasure()
    }

    // Wild Idea — the prepare spell. Tutor an instant or sorcery into hand.
    prepare("Wild Idea") {
        manaCost = "{3}{U}{R}"
        typeLine = "Sorcery"
        oracleText = "Search your library for an instant or sorcery card, reveal it, put it into " +
            "your hand, then shuffle."
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.InstantOrSorcery,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true,
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Justin Gerard"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/173157aa-712d-44f2-89ba-dd2511a07f26.jpg?1778165017"
    }
}
