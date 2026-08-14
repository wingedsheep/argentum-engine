package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Samut, the Driving Force — Aetherdrift #222
 * {3}{R}{G}{W} · Legendary Creature — Human Warrior Cleric · 4/5
 *
 * First strike, vigilance, haste
 * Start your engines!
 * Other creatures you control get +X/+0, where X is your speed.
 * Noncreature spells you cast cost {X} less to cast, where X is your speed.
 *
 * Both scaling abilities read the *same* value — `DynamicAmount.Speed(Player.You)` / the new
 * [CostReductionSource.YourSpeed] — through two different seams, because the engine reads costs and
 * stats through different machinery:
 *
 * - The lord is an ordinary dynamic layer-7c bonus ([GrantDynamicStatsEffect]), re-evaluated every
 *   projection, so the buff grows the instant your speed ticks up and `excludeSelf` keeps Samut out
 *   of its own "other creatures" clause. Toughness is a literal `Fixed(0)`, not the speed amount —
 *   the card grants +X/+0.
 * - Cost calculation never touches the layer system: `CostCalculator` scans the raw static-ability
 *   list for `ModifySpellCost`, so the reduction is expressed as a `ReduceGenericBy` over a
 *   [CostReductionSource], evaluated against the casting player at cost-calculation time.
 *
 * "No speed" is 0 (CR 702.179f), so before your engines start Samut is a plain 4/5 with no reduction
 * — Start your engines! then makes that self-correcting, since a state-based action raises your speed
 * to 1 as soon as Samut hits the battlefield.
 */
val SamutTheDrivingForce = card("Samut, the Driving Force") {
    manaCost = "{3}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Human Warrior Cleric"
    oracleText = "First strike, vigilance, haste\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on each of " +
        "your turns when an opponent loses life. Max speed is 4.)\n" +
        "Other creatures you control get +X/+0, where X is your speed.\n" +
        "Noncreature spells you cast cost {X} less to cast, where X is your speed."
    power = 4
    toughness = 5

    keywords(Keyword.FIRST_STRIKE, Keyword.VIGILANCE, Keyword.HASTE)

    startYourEngines()

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.OtherCreaturesYouControl,
            powerBonus = DynamicAmounts.speed(Player.You),
            toughnessBonus = DynamicAmount.Fixed(0),
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Noncreature),
            modification = CostModification.ReduceGenericBy(CostReductionSource.YourSpeed),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8efd8222-5c37-46d8-a2ec-1d7aae25320b.jpg?1783907852"
        ruling(
            "2025-02-07",
            "If an effect needs to know what a player's speed is and that player doesn't have a " +
                "speed, their speed is considered 0."
        )
        ruling(
            "2025-02-07",
            "Start your engines! isn't a triggered ability. Increasing your speed to 1 is something " +
                "that happens as a state-based action as soon as you control a permanent with the " +
                "ability."
        )
    }
}
