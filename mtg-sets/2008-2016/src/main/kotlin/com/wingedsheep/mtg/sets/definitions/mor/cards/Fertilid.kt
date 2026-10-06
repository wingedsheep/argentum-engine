package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Fertilid
 * {2}{G}
 * Creature — Elemental
 * 0/0
 * This creature enters with two +1/+1 counters on it.
 * {1}{G}, Remove a +1/+1 counter from this creature: Target player searches their library for a
 * basic land card, puts it onto the battlefield tapped, then shuffles.
 *
 * The search runs under [Effects.ForEachPlayer] scoped to the targeted player (same shape as
 * Fertilid's Favor), so that player searches, gets the tapped land, and shuffles their own library.
 */
val Fertilid = card("Fertilid") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 0
    toughness = 0
    oracleText = "This creature enters with two +1/+1 counters on it.\n" +
        "{1}{G}, Remove a +1/+1 counter from this creature: Target player searches their library " +
        "for a basic land card, puts it onto the battlefield tapped, then shuffles."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 2,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{G}"),
            Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 1)
        )
        val player = target(Targets.Player)
        effect = Effects.ForEachPlayer(
            player.asPlayer,
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad8055f1-cf3a-4db4-844e-b10bbbb25255.jpg?1783942779"
        ruling(
            "2020-11-10",
            "Although the targeted player doesn't need to find a basic land card if they don't want to, " +
                "that player must shuffle their library."
        )
    }
}
