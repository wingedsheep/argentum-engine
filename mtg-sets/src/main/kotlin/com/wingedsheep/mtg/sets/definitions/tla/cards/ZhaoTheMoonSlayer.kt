package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped
import com.wingedsheep.sdk.scripting.SetLandTypesForGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zhao, the Moon Slayer
 * {1}{R}
 * Legendary Creature — Human Soldier
 * 2/2
 *
 * Menace
 * Nonbasic lands enter tapped.
 * {7}: Put a conqueror counter on Zhao.
 * As long as Zhao has a conqueror counter on him, nonbasic lands are Mountains. (They lose all
 * other land types and abilities and have "{T}: Add {R}.")
 *
 * - "Nonbasic lands enter tapped" is a global [PermanentsEnterTapped] runtime replacement (the
 *   group counterpart of the self-only `EntersTapped`): every nonbasic land — either player's —
 *   is marked tapped as it enters. Unconditional while Zhao is on the battlefield.
 * - The {7} ability accumulates generic [Counters.CONQUEROR] counters on Zhao (as War Balloon
 *   accumulates fire counters), read only by the conditional static below.
 * - "Nonbasic lands are Mountains" is a [SetLandTypesForGroup] over all nonbasic lands, gated by
 *   [Conditions.SourceCounterCountAtLeast] so it applies only while Zhao has a conqueror counter.
 *   It realizes CR 305.7: Layer 4 replaces each nonbasic land's basic land subtypes with Mountain,
 *   and Layer 6 strips their other abilities; the Mountain's intrinsic "{T}: Add {R}" is derived
 *   from the projected subtype and survives, so the lands tap for red. Basic lands are unaffected.
 */
val ZhaoTheMoonSlayer = card("Zhao, the Moon Slayer") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Menace\n" +
        "Nonbasic lands enter tapped.\n" +
        "{7}: Put a conqueror counter on Zhao.\n" +
        "As long as Zhao has a conqueror counter on him, nonbasic lands are Mountains. (They lose " +
        "all other land types and abilities and have \"{T}: Add {R}.\")"

    keywords(Keyword.MENACE)

    // Nonbasic lands enter tapped. (All players' nonbasic lands — a global replacement effect.)
    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.NonbasicLand,
                to = Zone.BATTLEFIELD,
            )
        )
    )

    // {7}: Put a conqueror counter on Zhao.
    activatedAbility {
        cost = Costs.Mana("{7}")
        effect = Effects.AddCounters(Counters.CONQUEROR, 1, EffectTarget.Self)
    }

    // As long as Zhao has a conqueror counter on him, nonbasic lands are Mountains.
    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(Counters.CONQUEROR, 1)
        ability = SetLandTypesForGroup(
            filter = GroupFilter(GameObjectFilter.NonbasicLand),
            landTypes = setOf("Mountain"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Toraji"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1015bf5-de98-41f3-b6b1-ed95f7465944.jpg?1764121109"
    }
}
