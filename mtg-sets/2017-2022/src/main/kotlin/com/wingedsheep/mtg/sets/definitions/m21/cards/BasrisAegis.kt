package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Basri's Aegis
 * {2}{W}{W}
 * Sorcery
 * Put a +1/+1 counter on each of up to two target creatures. You may search your library and/or
 * graveyard for a card named Basri, Devoted Paladin, reveal it, and put it into your hand. If you
 * search your library this way, shuffle.
 *
 * The tutor is [Patterns.Library.searchLibraryAndOrGraveyard] with `optional = true`: one choice of
 * library / graveyard / both / don't search, shuffling only when the library was searched.
 */
val BasrisAegis = card("Basri's Aegis") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on each of up to two target creatures. You may search your " +
        "library and/or graveyard for a card named Basri, Devoted Paladin, reveal it, and put it into " +
        "your hand. If you search your library this way, shuffle."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        ) then Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Basri, Devoted Paladin"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "322"
        artist = "Paul Scott Canavan"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/577657ed-162f-4104-b0af-ee9733e90f20.jpg?1783930624"
    }
}
