package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Naked Time — Star Trek #159
 * {1}{R} · Enchantment — Saga
 *
 * I — Search your library for a basic Mountain card, put it onto the battlefield tapped, then shuffle.
 * II — Whenever a creature you control enters this turn, put a +1/+1 counter on it and it gains
 *      haste until end of turn.
 * III — Sacrifice a land.
 *
 * Chapter II is First Day of Class's turn-bounded, filter-scoped delayed trigger (`fireOnce = false`,
 * expiring at end of turn), installed from a Saga chapter the way Thunder of Unity's chapters do.
 * Chapter III is a non-targeted controller sacrifice (Springbloom Druid's shape); with no land it
 * does nothing.
 */
val TheNakedTime = card("The Naked Time") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Search your library for a basic Mountain card, put it onto the battlefield tapped, then shuffle.\n" +
        "II — Whenever a creature you control enters this turn, put a +1/+1 counter on it and it gains " +
        "haste until end of turn.\n" +
        "III — Sacrifice a land."

    sagaChapter(1) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.MOUNTAIN),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    sagaChapter(2) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters(),
            fireOnce = false,
            expiry = DelayedTriggerExpiry.EndOfTurn,
            effect = Effects.AddCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                1,
                EffectTarget.TriggeringEntity
            ) then Effects.GrantKeyword(Keyword.HASTE, EffectTarget.TriggeringEntity)
        )
    }

    sagaChapter(3) {
        effect = Effects.Sacrifice(GameObjectFilter.Land, count = 1, target = EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Jason Raish"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/627c2ff6-88be-4c37-a676-0eddf453e8c6.jpg?1793359584"
    }
}
