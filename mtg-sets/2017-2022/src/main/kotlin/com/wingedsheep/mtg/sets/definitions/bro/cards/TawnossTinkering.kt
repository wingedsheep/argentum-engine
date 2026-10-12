package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tawnos's Tinkering — The Brothers' War #191
 * {3}{G} · Instant
 *
 * Put two +1/+1 counters on target artifact, creature, or land you control. Untap that
 * permanent. If it isn't a creature, it becomes a 0/0 creature in addition to its other types.
 *
 * Counters, then untap, then an `If` gated on the target being noncreature at resolution
 * (after the counters, as worded) that permanently animates it as a 0/0 with no creature
 * types — the same shape as Donatello's "If it isn't a creature, it becomes a 0/0 Robot".
 * An empty `creatureTypes` leaves the permanent's subtypes untouched, and `BecomeCreature`
 * adds CREATURE while keeping the artifact/land types. No duration is printed, so it lasts
 * until the permanent leaves the battlefield.
 */
val TawnossTinkering = card("Tawnos's Tinkering") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put two +1/+1 counters on target artifact, creature, or land you control. " +
        "Untap that permanent. If it isn't a creature, it becomes a 0/0 creature in addition " +
        "to its other types."

    spell {
        val permanent = target(
            TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land).youControl())
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, permanent) then
            Effects.Untap(permanent) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Noncreature, permanent),
                then = Effects.BecomeCreature(
                    target = permanent,
                    power = 0,
                    toughness = 0,
                    duration = Duration.Permanent,
                ),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "191"
        artist = "Alex Stone"
        flavorText = "While his master tried to surpass nature, Tawnos aimed to capture its beauty."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acbd975d-52f8-4fba-bf97-b8837413edd8.jpg?1783920042"
        ruling(
            "2022-10-14",
            "Tawnos's Tinkering doesn't grant haste, so if a noncreature permanent that entered the " +
                "battlefield since the beginning of your upkeep becomes a creature this way, it can't " +
                "attack until your next turn. Any activated abilities that it has with a {T} symbol in " +
                "their cost also can't be activated until your next upkeep step.",
        )
    }
}
