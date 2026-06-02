package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Kavaron, Memorial World
 * Land — Planet
 * This land enters tapped.
 * {T}: Add {R}.
 * Station (Tap another creature you control: Put charge counters equal to its power on this Planet. Station only as a sorcery.)
 * 12+ | {1}{R}, {T}, Sacrifice a land: Create a 2/2 colorless Robot artifact creature token, then creatures you control get +1/+0 and gain haste until end of turn.
 */
val KavaronMemorialWorld = card("Kavaron, Memorial World") {
    typeLine = "Land — Planet"
    oracleText = "This land enters tapped.\n{T}: Add {R}.\nStation (Tap another creature you control: Put charge counters equal to its power on this Planet. Station only as a sorcery.)\n12+ | {1}{R}, {T}, Sacrifice a land: Create a 2/2 colorless Robot artifact creature token, then creatures you control get +1/+0 and gain haste until end of turn."

    // This land enters tapped
    replacementEffect(EntersTapped())

    // Basic mana ability: {T}: Add {R}
    activatedAbility {
        cost = Costs.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
    }

    // Station activated ability: tap another creature → add charge counters equal to its power
    activatedAbility {
        cost = AbilityCost.TapPermanents(
            count = 1,
            filter = GameObjectFilter.Creature,
            excludeSelf = true
        )
        effect = Effects.AddDynamicCounters(
            counterType = Counters.CHARGE,
            amount = DynamicAmount.EntityProperty(
                entity = EntityReference.TappedAsCost(),
                numericProperty = EntityNumericProperty.Power
            ),
            target = EffectTarget.Self
        )
        timing = TimingRule.SorcerySpeed
    }

    val charge12 = Compare(
        left = DynamicAmount.EntityProperty(
            entity = EntityReference.Source,
            numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
        ),
        operator = ComparisonOperator.GTE,
        right = DynamicAmount.Fixed(12)
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Land)
        )
        effect = ConditionalEffect(
            condition = charge12,
            effect = Effects.Composite(
                listOf(
                    // Create a 2/2 colorless Robot artifact creature token
                    CreateTokenEffect(
                        power = 2,
                        toughness = 2,
                        colors = setOf(), // colorless
                        creatureTypes = setOf("Robot"),
                        artifactToken = true,
                        imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130"
                    ),
                    // Creatures you control get +1/+0 and gain haste until end of turn
                    Effects.ForEachInGroup(
                        GroupFilter.AllCreaturesYouControl,
                        com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect(
                            powerModifier = DynamicAmount.Fixed(1),
                            toughnessModifier = DynamicAmount.Fixed(0),
                            target = EffectTarget.Self,
                            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn
                        )
                    ),
                    Effects.ForEachInGroup(
                        GroupFilter.AllCreaturesYouControl,
                        com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect(
                            keyword = Keyword.HASTE,
                            target = EffectTarget.Self,
                            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "255"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60f3ca25-9dcc-4781-bf7b-ab6736d8db29.jpg?1755341338"
    }
}
