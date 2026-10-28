package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vastwood Surge — Zendikar Rising #217
 * {3}{G} · Sorcery
 *
 * Kicker {4}
 * Search your library for up to two basic land cards, put them onto the battlefield tapped,
 * then shuffle. If this spell was kicked, put two +1/+1 counters on each creature you control.
 */
val VastwoodSurge = card("Vastwood Surge") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Kicker {4} (You may pay an additional {4} as you cast this spell.)\n" +
        "Search your library for up to two basic land cards, put them onto the battlefield tapped, " +
        "then shuffle. If this spell was kicked, put two +1/+1 counters on each creature you control."

    keywordAbility(KeywordAbility.kicker("{4}"))

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        ) then Effects.If(
            condition = WasKicked,
            then = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesYouControl,
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.IterationEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/6974363c-0d0d-4270-a25e-ab84b0c03f4e.jpg?1783929326"
    }
}
