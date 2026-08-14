package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mindspring Merfolk — Aetherdrift #51
 * {U} · Creature — Merfolk Wizard · 1/1
 *
 * Exhaust — {X}{U}{U}, {T}: Draw X cards. Put a +1/+1 counter on each Merfolk creature you control.
 *
 * `isExhaust = true` is the whole of CR 702.177 — the builder adds the once-per-object activation
 * restriction and the "Exhaust — " prefix, so this is an ordinary activated ability with an X in its
 * cost. X is chosen on activation and reaches the draw through [DynamicAmount.XValue].
 *
 * The counters go on **each** Merfolk you control, this creature included — it is itself a Merfolk,
 * and tapping it to pay the cost doesn't remove it from the battlefield — so the group filter keeps
 * the default `excludeSelf = false`. Ordering matters and matches the text: the draw happens first,
 * so a Merfolk drawn by this ability is not on the battlefield in time to get a counter.
 */
val MindspringMerfolk = card("Mindspring Merfolk") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "Exhaust — {X}{U}{U}, {T}: Draw X cards. Put a +1/+1 counter on each Merfolk " +
        "creature you control. (Activate each exhaust ability only once.)"
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{U}{U}"), Costs.Tap)
        isExhaust = true
        effect = Effects.Composite(
            Effects.DrawCards(DynamicAmount.XValue),
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.withSubtype("Merfolk").youControl()),
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Andreia Ugrai"
        flavorText = "The Invasion roused the reclusive jalpari from the depths. Since then, they " +
            "had fully integrated into Avishkari society."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6250b8b-1943-445f-ada9-30b41eb6d29b.jpg?1783907907"
        ruling("2025-02-07", "Exhaust abilities can be activated any time you could normally activate an ability.")
        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns to the battlefield, it becomes a new object so its exhaust " +
                "ability can be activated again."
        )
    }
}
