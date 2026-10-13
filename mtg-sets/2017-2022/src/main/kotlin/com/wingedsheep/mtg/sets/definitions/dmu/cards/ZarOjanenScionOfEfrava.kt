package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Zar Ojanen, Scion of Efrava
 * {3}{G}{W}
 * Legendary Creature — Cat Warrior
 * 4/4
 * Domain — Whenever Zar Ojanen becomes tapped, put a +1/+1 counter on each creature you control
 * with toughness less than the number of basic land types among lands you control.
 *
 * "Each creature you control" includes Zar itself. The group is read at resolution against
 * projected toughness, compared with domain evaluated for the ability's controller.
 */
val ZarOjanenScionOfEfrava = card("Zar Ojanen, Scion of Efrava") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Cat Warrior"
    power = 4
    toughness = 4
    oracleText = "Domain — Whenever Zar Ojanen becomes tapped, put a +1/+1 counter on each creature you control with toughness less than the number of basic land types among lands you control."

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter.Creature
                    .youControl()
                    .compareNumericProperty(
                        CardNumericProperty.TOUGHNESS,
                        ComparisonOperator.LT,
                        DynamicAmounts.domain(),
                    ),
            ),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Justine Cruz"
        flavorText = "\"I carry my home in my heart.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65c903e3-d622-415e-9144-a5b2f3f7b9ed.jpg?1783921273"
    }
}
