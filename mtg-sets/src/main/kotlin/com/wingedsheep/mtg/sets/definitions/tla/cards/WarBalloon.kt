package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * War Balloon
 * {2}{R}
 * Artifact — Vehicle
 * 4/3
 *
 * Flying
 * {1}: Put a fire counter on this Vehicle.
 * As long as this Vehicle has three or more fire counters on it, it's an artifact creature.
 * Crew 3
 *
 * The {1} activated ability accumulates generic [Counters.FIRE] counters on the source. The
 * conditional type change mirrors Phoenix Fleet Airship / Wedgelight Rammer: a Layer 4
 * [GrantCardType]("CREATURE") gated by [Conditions.SourceCounterCountAtLeast] so the Vehicle
 * counts as an artifact creature only while it has 3+ fire counters (CR 208.3 — it then uses
 * its printed 4/3). Crew 3 remains an independent alternative way to animate it.
 */
val WarBalloon = card("War Balloon") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 3
    oracleText = "Flying\n" +
        "{1}: Put a fire counter on this Vehicle.\n" +
        "As long as this Vehicle has three or more fire counters on it, it's an artifact creature.\n" +
        "Crew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING)

    // {1}: Put a fire counter on this Vehicle.
    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.AddCounters(Counters.FIRE, 1, EffectTarget.Self)
    }

    // Conditional type change: an artifact creature while it has 3+ fire counters.
    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(Counters.FIRE, 3)
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Matteo Bassini"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/6829b20d-c2fa-41a6-89ca-f21c522d8866.jpg?1764121090"
    }
}
