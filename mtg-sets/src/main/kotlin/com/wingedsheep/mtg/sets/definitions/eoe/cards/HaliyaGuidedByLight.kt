package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.GameEvent.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Haliya, Guided by Light
 * {2}{W}
 * Legendary Creature — Human Soldier
 * 3/3
 *
 * Whenever Haliya or another creature or artifact you control enters, you gain 1 life.
 * At the beginning of your end step, draw a card if you've gained 3 or more life this turn.
 * Warp {W} (You may cast this card from your hand for its warp cost. Exile this creature at the
 * beginning of the next end step, then you may cast it from exile on a later turn.)
 */
val HaliyaGuidedByLight = card("Haliya, Guided by Light") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    oracleText = "Whenever Haliya or another creature or artifact you control enters, you gain 1 life.\n" +
        "At the beginning of your end step, draw a card if you've gained 3 or more life this turn.\n" +
        "Warp {W} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"
    power = 3
    toughness = 3

    // Whenever Haliya or another creature or artifact you control enters, you gain 1 life.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.CreatureOrArtifact.youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.GainLife(1)
    }

    // At the beginning of your end step, draw a card if you've gained 3 or more life this turn.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Compare(
            DynamicAmounts.lifeGainedThisTurn(),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(3)
        )
        effect = Effects.DrawCards(1)
    }

    warp = "{W}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f7c63ae-5df3-410f-8643-b8c69133ca9d.jpg?1752946628"
    }
}
