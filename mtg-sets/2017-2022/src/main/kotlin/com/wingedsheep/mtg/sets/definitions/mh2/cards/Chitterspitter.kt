package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chitterspitter
 * {2}{G}
 * Artifact
 *
 * At the beginning of your upkeep, you may sacrifice a token. If you do, put an acorn counter on
 * this artifact.
 * Squirrels you control get +1/+1 for each acorn counter on this artifact.
 * {G}, {T}: Create a 1/1 green Squirrel creature token.
 *
 * "You may sacrifice a token. If you do, …" is [Effects.May] over [Effects.IfYouDo]: declining, or
 * having no token to sacrifice, adds no counter. Any token qualifies (Food, Treasure, Squirrel…).
 *
 * "Squirrels you control" is a bare tribal noun — every Squirrel *permanent* you control — so the
 * anthem filters [GameObjectFilter.Permanent] rather than `.Creature`. The bonus is a live static
 * sized by the acorn counters currently on Chitterspitter.
 */
val Chitterspitter = card("Chitterspitter") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, you may sacrifice a token. If you do, put an acorn " +
        "counter on this artifact.\n" +
        "Squirrels you control get +1/+1 for each acorn counter on this artifact.\n" +
        "{G}, {T}: Create a 1/1 green Squirrel creature token."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.SacrificeOwn(GameObjectFilter.Permanent.token()),
                then = Effects.AddCounters(CounterType.ACORN, 1, EffectTarget.Self),
                successCriterion = SuccessCriterion.PermanentsSacrificed
            )
        )
        description = "At the beginning of your upkeep, you may sacrifice a token. If you do, put an " +
            "acorn counter on this artifact."
    }

    staticAbility {
        val acorns = DynamicAmounts.countersOnSelf(CounterType.ACORN)
        ability = GrantDynamicStats(
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.SQUIRREL).youControl()),
            powerBonus = acorns,
            toughnessBonus = acorns
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Squirrel")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ee33ff3-d3d6-4519-9d9f-72e342d8b215.jpg?1783926833"
    }
}
