package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Long Road Home
 * {1}{W}
 * Instant
 * Exile target creature. At the beginning of the next end step, return that card to the
 * battlefield under its owner's control with a +1/+1 counter on it.
 *
 * Same shape as Otherworldly Journey: exile, then a delayed end-step `Move` of the same card with
 * `addCounterType`, so it returns as a new object already carrying the counter. "Under its owner's
 * control" is the battlefield move's default for a returning card; an exiled token ceases to exist
 * and never comes back.
 */
val LongRoadHome = card("Long Road Home") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature. At the beginning of the next end step, return that card to " +
        "the battlefield under its owner's control with a +1/+1 counter on it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Exile(creature) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Move(
                    creature,
                    Zone.BATTLEFIELD,
                    addCounterType = CounterType.PLUS_ONE_PLUS_ONE
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Sidharth Chaturvedi"
        flavorText = "The person who finishes a journey is rarely the same as the one who starts it."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d568323c-b631-48b6-b6da-4dc29d330a12.jpg?1783937513"
        ruling("2016-07-13", "If a double-faced card is exiled this way, it'll return with its front face up. It gets a +1/+1 counter even if it isn't a creature.")
        ruling("2016-07-13", "If that creature is exiled during an end step, it won't come back until the next turn's end step.")
        ruling("2016-07-13", "If a creature token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2016-07-13", "At the time the creature is exiled, Auras attached to it will be put into their owners' graveyards. Any Equipment will become unattached and remain on the battlefield. Any counters on it at that time will cease to exist.")
        ruling("2016-07-13", "If a melded permanent is exiled this way, the two cards will both return with their front faces up. Each gets a +1/+1 counter.")
    }
}
