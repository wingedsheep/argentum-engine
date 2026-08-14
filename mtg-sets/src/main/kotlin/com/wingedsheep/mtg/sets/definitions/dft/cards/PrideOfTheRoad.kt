package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Pride of the Road — Aetherdrift #24
 * {3}{W} · Creature — Zombie Cat Warrior · 2/5
 *
 * Vigilance
 * Start your engines!
 * Max speed — At the beginning of combat on your turn, target creature or Vehicle you control gains
 * double strike until end of turn.
 *
 * "Max speed — [ability]" is "as long as you have max speed, this object has [ability]"
 * (CR 702.178a), so the [maxSpeed] block gates the triggered ability with a `triggerCondition`
 * (CR 603.4): checked when the trigger would fire *and* again on resolution. Dropping below speed 4
 * between the two — which can't normally happen, speed only rises — would correctly do nothing.
 *
 * The target is a plain creature-or-Vehicle-you-control permanent target, so an un-crewed Vehicle is
 * legal (Vehicles are targetable whether or not they're currently creatures) and keeps the double
 * strike if it's crewed later the same turn.
 */
val PrideOfTheRoad = card("Pride of the Road") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Zombie Cat Warrior"
    power = 2
    toughness = 5
    oracleText = "Vigilance\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on each of " +
        "your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — At the beginning of combat on your turn, target creature or Vehicle you " +
        "control gains double strike until end of turn."

    keywords(Keyword.VIGILANCE)
    startYourEngines()

    maxSpeed {
        triggeredAbility {
            trigger = Triggers.BeginCombat
            val t = target(
                "creature or Vehicle you control",
                TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle.youControl()))
            )
            effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, t)
            description = "At the beginning of combat on your turn, target creature or Vehicle " +
                "you control gains double strike until end of turn."
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Alfonso Santano"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4172222f-d871-4354-9a02-7af0001d8956.jpg?1783907916"
    }
}
