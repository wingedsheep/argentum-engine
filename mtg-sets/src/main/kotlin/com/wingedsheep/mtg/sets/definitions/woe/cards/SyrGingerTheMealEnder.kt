package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Syr Ginger, the Meal Ender
 * {2}
 * Legendary Artifact Creature — Food Knight
 * 3/1
 *
 * Syr Ginger has trample, hexproof, and haste as long as an opponent controls a planeswalker.
 * Whenever another artifact you control is put into a graveyard from the battlefield, put a
 * +1/+1 counter on Syr Ginger and scry 1.
 * {2}, {T}, Sacrifice Syr Ginger: You gain life equal to its power.
 *
 * Three separate `staticAbility { }` blocks rather than one: a static ability is one continuous
 * modification, and the DSL takes exactly one [com.wingedsheep.sdk.scripting.StaticAbility] per
 * block. All three share the same [Conditions.OpponentControls] gate, so they wink in and out
 * together as planeswalkers come and go — a condition, not a one-shot grant.
 *
 * The death payoff is [Triggers.leavesBattlefield] scoped to `Artifact.youControl()` with
 * [TriggerBinding.OTHER] for "another" — Syr Ginger is itself an artifact, so without the OTHER
 * binding sacrificing it to its own last ability would grow it on the way out. Any artifact you
 * control counts, whether it died in combat, was sacrificed, or was destroyed; Food tokens
 * eaten by their own ability are the intended engine.
 *
 * The sacrifice ability reads [DynamicAmounts.sourcePower], which is `LIVE_THEN_LKI` for
 * [com.wingedsheep.sdk.scripting.values.EntityReference.Source]: the sacrifice is a *cost*, so
 * Syr Ginger is already in the graveyard when the ability resolves and the read falls through to
 * the snapshot captured at cost-payment time. That is exactly the printed ruling — "use its power
 * from when it was last on the battlefield" — so counters accumulated by the middle ability, and
 * any pumps, are all counted. A power of 0 or less gains no life.
 */
val SyrGingerTheMealEnder = card("Syr Ginger, the Meal Ender") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Food Knight"
    power = 3
    toughness = 1
    oracleText = "Syr Ginger has trample, hexproof, and haste as long as an opponent controls a " +
        "planeswalker.\n" +
        "Whenever another artifact you control is put into a graveyard from the battlefield, put a " +
        "+1/+1 counter on Syr Ginger and scry 1.\n" +
        "{2}, {T}, Sacrifice Syr Ginger: You gain life equal to its power."

    staticAbility {
        condition = Conditions.OpponentControls(GameObjectFilter.Planeswalker)
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.Self)
    }

    staticAbility {
        condition = Conditions.OpponentControls(GameObjectFilter.Planeswalker)
        ability = GrantKeyword(Keyword.HEXPROOF, Filters.Self)
    }

    staticAbility {
        condition = Conditions.OpponentControls(GameObjectFilter.Planeswalker)
        ability = GrantKeyword(Keyword.HASTE, Filters.Self)
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Artifact.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.OTHER,
        )
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Patterns.Library.scry(1),
        )
        description = "Whenever another artifact you control is put into a graveyard from the " +
            "battlefield, put a +1/+1 counter on Syr Ginger and scry 1."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.GainLife(DynamicAmounts.sourcePower())
        description = "{2}, {T}, Sacrifice Syr Ginger: You gain life equal to its power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Michal Ivan"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fbdba12-1369-41ae-b0e9-c405c0f0a2e5.jpg?1783915057"

        ruling(
            "2023-09-01",
            "For Syr Ginger's last ability, use its power from when it was last on the battlefield " +
                "to determine how much life is gained. If that power was 0 or less, you gain no life."
        )
    }
}
