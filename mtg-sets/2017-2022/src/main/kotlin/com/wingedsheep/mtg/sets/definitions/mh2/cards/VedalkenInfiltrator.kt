package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Vedalken Infiltrator — Modern Horizons 2 #73
 * {1}{U} · Creature — Vedalken Rogue · 1 / 3
 *
 * This creature can't be blocked.
 * Metalcraft — This creature gets +1/+0 as long as you control three or more artifacts.
 *
 * Two unrelated statics, each in its own vocabulary.
 *
 * "Can't be blocked" is not a [com.wingedsheep.sdk.core.Keyword] — there is no keyword for it to
 * abbreviate — but an evasion [AbilityFlag], the same one Phantom Warrior carries. The blocking
 * rules read the flag directly when legal blocks are enumerated.
 *
 * "Metalcraft" is an *ability word*: pure flavour with no rules meaning of its own (CR 207.2c), so
 * there is no `Keyword.METALCRAFT` and nothing but the oracle line records it. What it introduces is
 * an ordinary [ConditionalStaticAbility] — a layer-7c (CR 613.4c) [ModifyStats] over `GroupFilter.source()`
 * gated by [Conditions.YouControlAtLeast], recomputed at projection so the bonus appears and
 * disappears with the third artifact rather than being latched at any point in time.
 */
val VedalkenInfiltrator = card("Vedalken Infiltrator") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Rogue"
    power = 1
    toughness = 3
    oracleText = "This creature can't be blocked.\n" +
        "Metalcraft — This creature gets +1/+0 as long as you control three or more artifacts."

    flags(AbilityFlag.CANT_BE_BLOCKED)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 0, filter = GroupFilter.source()),
            condition = Conditions.YouControlAtLeast(3, GameObjectFilter.Artifact),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Izzy"
        flavorText = "\"An expert always keeps their tools close at hand.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9ddfc6a8-7880-4810-8c0a-d9ea931138f2.jpg?1783926866"
    }
}
