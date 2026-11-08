package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Backwoods Survivalists (Eldritch Moon #150)
 * {3}{G}
 * Creature — Human Warrior
 * 4/3
 *
 * Delirium — This creature gets +1/+1 and has trample as long as there are four or more card
 * types among cards in your graveyard.
 *
 * Two conditional statics (+1/+1, trample) gated on the graveyard's distinct card-type count.
 */
val BackwoodsSurvivalists = card("Backwoods Survivalists") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 4
    toughness = 3
    oracleText = "Delirium — This creature gets +1/+1 and has trample as long as there are " +
        "four or more card types among cards in your graveyard."

    val delirium = Conditions.CompareAmounts(
        DynamicAmounts.zone(Player.You, Zone.GRAVEYARD).distinctTypes(),
        ComparisonOperator.GTE,
        4
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
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Chris Rallis"
        flavorText = "Paranoia has driven some Kessig communities to hunt down anything—and anyone—that " +
            "travels through their woods."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a3a65be-0ca1-437f-aafe-d96e8fe428ad.jpg"
    }
}
