package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gatebreaker Ram — RNA #126
 * {2}{G} Creature — Sheep (2/2), Uncommon
 *
 * This creature gets +1/+1 for each Gate you control.
 * As long as you control two or more Gates, this creature has vigilance and trample.
 */
private val gateYouControl = GameObjectFilter.Permanent.withSubtype(Subtype.GATE).youControl()

val GatebreakerRam = card("Gatebreaker Ram") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Sheep"
    power = 2
    toughness = 2
    oracleText = "This creature gets +1/+1 for each Gate you control.\n" +
        "As long as you control two or more Gates, this creature has vigilance and trample."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, gateYouControl),
            toughnessBonus = DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, gateYouControl),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source()),
            condition = Conditions.YouControlAtLeast(2, gateYouControl),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source()),
            condition = Conditions.YouControlAtLeast(2, gateYouControl),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Even Amundsen"
        flavorText = "So-called \"battering rams\" pale in comparison to the real thing."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ef592d1-e5e1-4252-8741-402c32d65dfd.jpg?1783933671"
    }
}
