package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invert // Invent (GRN 228) — split-layout spell (CR 709).
 *
 * Invert {U/R} — Instant
 *   Switch the power and toughness of each of up to two target creatures until end of turn.
 *
 * Invent {4}{U}{R} — Instant
 *   Search your library for an instant card and/or a sorcery card, reveal them, put them into your
 *   hand, then shuffle.
 *
 * Invert switches each target's own power and toughness separately (2018-10-05 ruling), so it is a
 * per-target [Effects.SwitchPowerToughness]. "An instant card and/or a sorcery card" is two
 * independent up-to-one searches ([com.wingedsheep.mtg.sets.definitions.khm.cards.AxgardArmory]'s
 * shape) — either may find nothing — with a single shuffle after the second.
 */
val InvertInvent = card("Invert // Invent") {
    layout = CardLayout.SPLIT
    colorIdentity = "UR"

    face("Invert") {
        manaCost = "{U/R}"
        typeLine = "Instant"
        oracleText = "Switch the power and toughness of each of up to two target creatures until end of turn."

        spell {
            targets(TargetFilter.Creature, count = 2, optional = true)
            effect = Effects.ForEachTarget(
                Effects.SwitchPowerToughness(EffectTarget.ContextTarget(0))
            )
        }
    }

    face("Invent") {
        manaCost = "{4}{U}{R}"
        typeLine = "Instant"
        oracleText = "Search your library for an instant card and/or a sorcery card, reveal them, " +
            "put them into your hand, then shuffle."

        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Instant,
                reveal = true,
                shuffleAfter = false
            ) then Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Sorcery,
                reveal = true,
                shuffleAfter = true
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/054a4e4f-8baa-41cf-b24c-d068e8b9a070.jpg?1783934111"
        ruling("2018-10-05", "While resolving Invent, you could put no cards, an instant card, a sorcery card, or an instant card and a sorcery card into your hand.")
        ruling("2018-10-05", "If you chose two targets, Invert switches the power and toughness of one creature and it switches the power and toughness of another creature. It doesn't switch one creature's power with another creature's power.")
        ruling("2021-03-19", "Effects that switch a creature's power and toughness apply after all other effects, regardless of when those effects began to apply. For instance, if you target a 1/2 creature then give it +2/+0 later in the turn, it's a 2/3 creature, not a 4/1 creature.")
    }
}
