package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaPayment

/**
 * Jetmir's Fixer
 * {R}{G}
 * Creature — Cat Warrior
 * 2/2
 * {R}{G}: This creature gets +1/+1 until end of turn. If mana from a Treasure was spent to
 * activate this ability, put a +1/+1 counter on this creature instead.
 *
 * The Treasure check reads the ability's own saved payment
 * (`manaSpentFromSubtype(TREASURE, ACTIVATION)`), the same provenance Mr. House counts.
 */
val JetmirsFixer = card("Jetmir's Fixer") {
    manaCost = "{R}{G}"
    colorIdentity = "GR"
    typeLine = "Creature — Cat Warrior"
    oracleText = "{R}{G}: This creature gets +1/+1 until end of turn. If mana from a Treasure was spent to activate this ability, put a +1/+1 counter on this creature instead."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{R}{G}")
        effect = Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE, ManaPayment.ACTIVATION),
                ComparisonOperator.GTE,
                1,
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            otherwise = Effects.ModifyStats(1, 1, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "John Thacker"
        flavorText = "When you lose Jetmir's trust, his family makes sure you lose everything else."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e5aa701-28e8-47fa-8c26-4da2f943f582.jpg?1783923081"
    }
}
