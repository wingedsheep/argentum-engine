package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
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
 *
 * Put a +1/+1 counter on each of up to two target creatures. You may search your library and/or
 * graveyard for a card named Basri, Devoted Paladin, reveal it, and put it into your hand. If you
 * search your library this way, shuffle.
 *
 * The counters are an optional `count = 2` target slot fanned out with `ForEachTarget`. The search
 * is the shared library + graveyard up-to-one pick (Grand Master of Flowers' identical
 * "search your library and/or graveyard for a card named …" shape). With no targets chosen the
 * spell still resolves and only searches (ruling); if every chosen target is illegal on
 * resolution the spell doesn't resolve and there's no search.
 */
val BasrisAegis = card("Basri's Aegis") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on each of up to two target creatures. You may search your library " +
        "and/or graveyard for a card named Basri, Devoted Paladin, reveal it, and put it into your hand. " +
        "If you search your library this way, shuffle."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        ) then Patterns.Library.searchMultipleZones(
            zones = listOf(Zone.LIBRARY, Zone.GRAVEYARD),
            filter = GameObjectFilter.Any.named("Basri, Devoted Paladin"),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "322"
        artist = "Paul Scott Canavan"
        flavorText = "\"As long as we survive, we can rebuild.\"\n—Basri Ket"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/577657ed-162f-4104-b0af-ee9733e90f20.jpg?1783930624"
        ruling("2020-06-23", "You can cast Basri's Aegis without any target creatures. You'll just search for Basri, Devoted Paladin.")
        ruling("2020-06-23", "If do you choose targets and each target creature is an illegal target by the time Basri's Aegis tries to resolve, the spell won't resolve. You won't search for Basri. If just one target is still legal, however, you do put a +1/+1 counter on it and search for Basri.")
    }
}
