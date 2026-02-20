package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tephraderm
 * {4}{R}
 * Creature — Beast
 * 4/5
 * Whenever a creature deals damage to Tephraderm, Tephraderm deals that much damage to that creature.
 * Whenever a spell deals damage to Tephraderm, Tephraderm deals that much damage to that spell's controller.
 */
val Tephraderm = card("Tephraderm") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 5
    oracleText = "Whenever a creature deals damage to Tephraderm, Tephraderm deals that much damage to that creature.\nWhenever a spell deals damage to Tephraderm, Tephraderm deals that much damage to that spell's controller."

    triggeredAbility {
        trigger = Triggers.DamagedByCreature
        effect = DealDamageEffect(
            amount = DynamicAmount.TriggerDamageAmount,
            target = EffectTarget.TriggeringEntity
        )
    }

    triggeredAbility {
        trigger = Triggers.DamagedBySpell
        effect = DealDamageEffect(
            amount = DynamicAmount.TriggerDamageAmount,
            target = EffectTarget.ControllerOfTriggeringEntity
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "239"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/large/front/4/1/41b65eba-140b-4c1d-b796-8134b7c1ede8.jpg?1562910455"
    }
}
