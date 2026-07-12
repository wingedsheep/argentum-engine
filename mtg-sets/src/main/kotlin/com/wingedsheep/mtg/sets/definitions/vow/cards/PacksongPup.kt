package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Packsong Pup
 * {1}{G}
 * Creature — Wolf
 * 1/1
 * At the beginning of combat on your turn, if you control another Wolf or Werewolf, put a +1/+1
 * counter on this creature.
 * When this creature dies, you gain life equal to its power.
 */
val PacksongPup = card("Packsong Pup") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    oracleText = "At the beginning of combat on your turn, if you control another Wolf or Werewolf, put a +1/+1 counter on this creature.\nWhen this creature dies, you gain life equal to its power."
    power = 1
    toughness = 1
    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Conditions.YouControl(
            filter = GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.WOLF, Subtype.WEREWOLF)),
            excludeSelf = true
        )
        effect = AddCountersEffect(counterType = Counters.PLUS_ONE_PLUS_ONE, count = 1, target = EffectTarget.Self)
    }
    triggeredAbility {
        trigger = Triggers.Dies
        effect = GainLifeEffect(DynamicAmounts.sourcePower())
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "April Prime"
        flavorText = "A wolf pup is always dangerous, because a wolf pup is never alone."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d43d9686-a5e4-413b-8a34-3430788dd1b9.jpg?1782703044"
    }
}
