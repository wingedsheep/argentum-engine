package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoyaltyActivationPermission
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Wandering Emperor
 * {2}{W}{W}
 * Legendary Planeswalker
 *
 * Flash
 * As long as The Wandering Emperor entered this turn, you may activate her loyalty abilities any
 * time you could cast an instant.
 * +1: Put a +1/+1 counter on up to one target creature. It gains first strike until end of turn.
 * −1: Create a 2/2 white Samurai creature token with vigilance.
 * −2: Exile target tapped creature. You gain 2 life.
 *
 * The timing clause is a self-scoped instant-speed [LoyaltyActivationPermission] gated on
 * `Conditions.SourceEnteredThisTurn`; it lifts only the timing half of CR 606.3, so she still
 * activates one loyalty ability per turn.
 */
val TheWanderingEmperor = card("The Wandering Emperor") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker"
    startingLoyalty = 3
    oracleText = "Flash\n" +
        "As long as The Wandering Emperor entered this turn, you may activate her loyalty abilities any time you could cast an instant.\n" +
        "+1: Put a +1/+1 counter on up to one target creature. It gains first strike until end of turn.\n" +
        "−1: Create a 2/2 white Samurai creature token with vigilance.\n" +
        "−2: Exile target tapped creature. You gain 2 life."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = LoyaltyActivationPermission(GameObjectFilter.Any.sourceItself(), instantSpeed = true),
            condition = Conditions.SourceEnteredThisTurn,
        )
    }

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
    }

    loyaltyAbility(-1) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Samurai"),
            keywords = setOf(Keyword.VIGILANCE),
        )
    }

    loyaltyAbility(-2) {
        val creature = target(TargetFilter.Creature.tapped())
        effect = Effects.Exile(creature) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "42"
        artist = "Tommy Arnold"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fab2d8a9-ab4c-4225-a570-22636293c17d.jpg?1783923909"
        ruling("2022-02-18", "You may still only activate one of The Wandering Emperor's loyalty abilities on the turn she entered the battlefield.")
    }
}
