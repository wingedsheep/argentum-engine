package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lifecraft Awakening — Aether Revolt #112
 * {X}{G} · Instant
 *
 * Put X +1/+1 counters on target artifact you control. If it isn't a creature or Vehicle, it
 * becomes a 0/0 Construct artifact creature.
 *
 * Tawnos's Tinkering's shape: X counters, then an `If` checked at resolution (after the
 * counters) on the target being a noncreature non-Vehicle, which permanently animates it as a
 * base 0/0 Construct. `BecomeCreature` adds CREATURE alongside the artifact type it already has;
 * no duration is printed, so it lasts indefinitely.
 */
val LifecraftAwakening = card("Lifecraft Awakening") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put X +1/+1 counters on target artifact you control. If it isn't a creature or " +
        "Vehicle, it becomes a 0/0 Construct artifact creature."

    spell {
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.youControl()))
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.xValue(),
            target = artifact,
        ) then Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Noncreature.notSubtype(Subtype.VEHICLE),
                artifact,
            ),
            then = Effects.BecomeCreature(
                target = artifact,
                power = 0,
                toughness = 0,
                creatureTypes = setOf("Construct"),
                duration = Duration.Permanent,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Raoul Vitale"
        flavorText = "\"It was made, yes, but that does not mean it cannot grow.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63bc60c8-f0a3-4bbf-b570-7d98d0a3e2b0.jpg?1783936744"

        ruling(
            "2017-02-09",
            "You can cast Lifecraft Awakening declaring X as 0. If the artifact isn't a creature or Vehicle, " +
                "it'll become a 0/0 creature and, unless another effect modifies its toughness, be put into " +
                "your graveyard soon after.",
        )
        ruling(
            "2017-02-09",
            "You can put +1/+1 counters on a noncreature Vehicle this way. They'll be put there, apply if the " +
                "Vehicle becomes a creature, and remain there when it stops being a creature.",
        )
        ruling(
            "2017-02-09",
            "If the artifact isn't a creature or Vehicle as Lifecraft Awakening resolves, the artifact " +
                "becomes a creature indefinitely.",
        )
        ruling(
            "2017-02-09",
            "If an Equipment becomes a creature, it becomes unattached and it can't be attached to a creature.",
        )
    }
}
