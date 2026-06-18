package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wildfire Wickerfolk — Duskmourn: House of Horror #239
 * {R}{G}
 * Artifact Creature — Scarecrow
 * 3/2
 *
 * Haste
 * Delirium — This creature gets +1/+1 and has trample as long as there are four or more
 * card types among cards in your graveyard.
 *
 * Delirium is an ability word (no rules meaning of its own); the buff is a pair of
 * [ConditionalStaticAbility] gated by a graveyard distinct-card-type count ≥ 4. Card types are
 * artifact, battle, creature, enchantment, instant, kindred, land, planeswalker, sorcery
 * ([Aggregation.DISTINCT_TYPES] over the graveyard) — supertypes (legendary/basic/snow) and
 * subtypes don't count.
 */
val WildfireWickerfolk = card("Wildfire Wickerfolk") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Artifact Creature — Scarecrow"
    power = 3
    toughness = 2
    oracleText = "Haste\nDelirium — This creature gets +1/+1 and has trample as long as there are " +
        "four or more card types among cards in your graveyard."

    keywords(Keyword.HASTE)

    val delirium = Compare(
        DynamicAmount.AggregateZone(
            Player.You,
            Zone.GRAVEYARD,
            GameObjectFilter.Any,
            Aggregation.DISTINCT_TYPES
        ),
        ComparisonOperator.GTE,
        DynamicAmount.Fixed(4)
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, GroupFilter.source()),
            condition = delirium
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source()),
            condition = delirium
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "J.Lonnee"
        flavorText = "\"Isn't fire supposed to *kill* those things?\"\n—Tyvar"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c7fbc6e-09c6-4f0e-92e2-766aae950b3d.jpg?1726286766"
    }
}
