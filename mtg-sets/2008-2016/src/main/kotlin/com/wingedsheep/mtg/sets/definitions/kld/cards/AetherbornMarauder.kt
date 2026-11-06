package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aetherborn Marauder
 * {3}{B}
 * Creature — Aetherborn Rogue
 * 2/2
 *
 * Flying, lifelink
 * When this creature enters, move any number of +1/+1 counters from other permanents you control
 * onto this creature.
 *
 * - As the trigger resolves it asks, per other permanent you control carrying +1/+1 counters, how
 *   many of them move — zero included, so "any number" is the sum of those answers. Other counter
 *   kinds stay put. The counters land together as one move, so Hardened Scales adds one, not one
 *   per permanent.
 * - If the Marauder has left the battlefield by then, nothing moves.
 */
val AetherbornMarauder = card("Aetherborn Marauder") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Aetherborn Rogue"
    power = 2
    toughness = 2
    oracleText = "Flying, lifelink\n" +
        "When this creature enters, move any number of +1/+1 counters from other permanents you " +
        "control onto this creature."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MoveChosenCountersToTarget(
            source = EffectTarget.GroupRef(
                GroupFilter(
                    GameObjectFilter.Permanent.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)
                ).other()
            ),
            destination = EffectTarget.Self,
            counterType = CounterType.PLUS_ONE_PLUS_ONE
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Kieran Yanner"
        flavorText = "\"I don't know what that is or who it belongs to, but it's shiny and I want it.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fce02ef8-1731-43a9-9bd0-9ec592e3883f.jpg?1783937211"
    }
}
