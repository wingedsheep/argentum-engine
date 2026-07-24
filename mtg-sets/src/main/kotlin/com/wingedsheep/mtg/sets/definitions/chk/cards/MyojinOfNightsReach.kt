package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Myojin of Night's Reach
 * {5}{B}{B}{B}
 * Legendary Creature — Spirit
 * 5/2
 *
 * Myojin of Night's Reach enters with a divinity counter on it if you cast it from your hand.
 * Myojin of Night's Reach has indestructible as long as it has a divinity counter on it.
 * Remove a divinity counter from Myojin of Night's Reach: Each opponent discards their hand.
 */
val MyojinOfNightsReach = card("Myojin of Night's Reach") {
    manaCost = "{5}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Spirit"
    power = 5
    toughness = 2
    oracleText = "Myojin of Night's Reach enters with a divinity counter on it if you cast it from your hand.\n" +
        "Myojin of Night's Reach has indestructible as long as it has a divinity counter on it.\n" +
        "Remove a divinity counter from Myojin of Night's Reach: Each opponent discards their hand."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.DIVINITY),
            count = 1,
            selfOnly = true,
            condition = Conditions.WasCastFromHand,
        )
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE),
            condition = Conditions.SourceHasCounter(CounterTypeFilter.Named(Counters.DIVINITY)),
        )
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(Counters.DIVINITY)
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            listOf(
                Patterns.Hand.discardCards(
                    DynamicAmount.Count(Player.You, Zone.HAND),
                    EffectTarget.Controller,
                )
            ),
        )
        description = "Remove a divinity counter from this creature: Each opponent discards their hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13a295b0-535e-4c2d-879d-62603d1f2f1b.jpg?1783944311"
        ruling(
            "2024-11-08",
            "In a Commander game where this card is your commander, casting it from the command zone " +
                "does not count as casting it from your hand.",
        )
    }
}
