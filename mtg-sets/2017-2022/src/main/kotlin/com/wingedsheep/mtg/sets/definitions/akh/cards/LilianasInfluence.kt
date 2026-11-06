package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liliana's Influence — Amonkhet #277 (planeswalker deck)
 * {4}{B}{B} · Sorcery
 *
 * Put a -1/-1 counter on each creature you don't control. You may search your library and/or
 * graveyard for a card named Liliana, Death Wielder, reveal it, and put it into your hand. If you
 * search your library this way, shuffle.
 *
 * "Each creature you don't control" is every creature an opponent controls (Archfiend of Ifnir's
 * group); the tutor is the planeswalker-deck search shape (Vraska's Scorn).
 */
val LilianasInfluence = card("Liliana's Influence") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Put a -1/-1 counter on each creature you don't control. You may search your library " +
        "and/or graveyard for a card named Liliana, Death Wielder, reveal it, and put it into your hand. " +
        "If you search your library this way, shuffle."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.opponentControls()),
            Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.IterationEntity)
        ) then
            Patterns.Library.searchLibraryAndOrGraveyard(
                filter = GameObjectFilter.Any.named("Liliana, Death Wielder"),
                destination = SearchDestination.HAND,
                reveal = true,
                optional = true
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "277"
        inBooster = false
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28cdf48e-e0d4-45ca-b48d-74ada54ba62e.jpg?1783936436"
    }
}
