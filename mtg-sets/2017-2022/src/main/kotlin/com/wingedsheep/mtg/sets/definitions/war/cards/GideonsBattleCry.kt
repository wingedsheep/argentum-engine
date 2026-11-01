package com.wingedsheep.mtg.sets.definitions.war.cards

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
 * Gideon's Battle Cry — War of the Spark #267 (canonical printing; planeswalker deck)
 * {2}{W}{W} · Sorcery
 *
 * Put a +1/+1 counter on each creature you control. You may search your library and/or graveyard
 * for a card named Gideon, the Oathsworn, reveal it, and put it into your hand. If you search your
 * library this way, shuffle.
 *
 * The counters are Ajani Goldmane's −1 group shape; the search half is the planeswalker-deck tutor
 * shared with Jace's Ruse and Oko's Hospitality.
 */
val GideonsBattleCry = card("Gideon's Battle Cry") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on each creature you control. You may search your library and/or " +
        "graveyard for a card named Gideon, the Oathsworn, reveal it, and put it into your hand. If you " +
        "search your library this way, shuffle."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        ) then Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Gideon, the Oathsworn"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "267"
        artist = "Zoltan Boros"
        flavorText = "\"Break their formation! Thin their ranks! I'm right behind you!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52de8fc6-6d9b-402e-a6c8-4c39b0c92f07.jpg?1783933360"
        ruling(
            "2019-05-03",
            "You may cast Gideon's Battle Cry while you control no creatures. You'll still search for Gideon."
        )
    }
}
