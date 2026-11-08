package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Desperate Sentry (Eldritch Moon #21)
 * {2}{W}
 * Creature — Human Soldier
 * 1/2
 *
 * When this creature dies, create a 3/2 colorless Eldrazi Horror creature token.
 * Delirium — This creature gets +3/+0 as long as there are four or more card types among cards in
 * your graveyard.
 */
val DesperateSentry = card("Desperate Sentry") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "When this creature dies, create a 3/2 colorless Eldrazi Horror creature token.\n" +
        "Delirium — This creature gets +3/+0 as long as there are four or more card types among " +
        "cards in your graveyard."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Horror")
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(3, 0, GroupFilter.source()),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.zone(Player.You, Zone.GRAVEYARD).distinctTypes(),
                ComparisonOperator.GTE,
                4
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4135c619-fcff-4339-b778-31dd83e68fcb.jpg"
    }
}
