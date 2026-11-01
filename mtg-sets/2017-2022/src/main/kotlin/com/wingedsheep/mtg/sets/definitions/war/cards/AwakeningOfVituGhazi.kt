package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Awakening of Vitu-Ghazi — War of the Spark #152
 * {3}{G}{G} · Instant
 *
 * Put nine +1/+1 counters on target land you control. It becomes a legendary 0/0 Elemental
 * creature with haste named Vitu-Ghazi. It's still a land.
 *
 * The animation is durationless (`Duration.Permanent`, per the rulings) and keeps the land's
 * existing types and abilities: `BecomeCreature` adds CREATURE plus the LEGENDARY supertype and
 * the Elemental creature type. The rename rides `BecomeArtifact` with every artifact knob off
 * (`cardTypes`/`subtypes`/`colors` null, `loseAllAbilities = false`) — the SDK's projected Layer 3
 * `SetName`, as on Tenth District Hero — so the land's name is only "Vitu-Ghazi".
 */
val AwakeningOfVituGhazi = card("Awakening of Vitu-Ghazi") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put nine +1/+1 counters on target land you control. It becomes a legendary 0/0 " +
        "Elemental creature with haste named Vitu-Ghazi. It's still a land."

    spell {
        val land = target(TargetFilter.LandYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 9, land) then
            Effects.BecomeCreature(
                target = land,
                power = 0,
                toughness = 0,
                keywords = setOf(Keyword.HASTE),
                creatureTypes = setOf("Elemental"),
                addTypes = setOf("LEGENDARY"),
                duration = Duration.Permanent,
            ) then
            Effects.BecomeArtifact(
                target = land,
                cardTypes = null,
                subtypes = null,
                colors = null,
                loseAllAbilities = false,
                name = "Vitu-Ghazi",
                duration = Duration.Permanent,
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Jaime Jones"
        flavorText = "The Eternal armies advanced on Vitu-Ghazi. At Nissa's command, Vitu-Ghazi " +
            "advanced on them."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b42b2fb4-8016-462c-8764-2639adec931f.jpg?1783933417"
        ruling("2019-05-03", "Awakening of Vitu-Ghazi doesn't untap the land that becomes a creature.")
        ruling(
            "2019-05-03",
            "The target land loses the name it had, and its name is just Vitu-Ghazi. It keeps any " +
                "types and abilities it had."
        )
        ruling(
            "2019-05-03",
            "Awakening of Vitu-Ghazi's effect lasts indefinitely. It doesn't wear off during the " +
                "cleanup step."
        )
    }
}
