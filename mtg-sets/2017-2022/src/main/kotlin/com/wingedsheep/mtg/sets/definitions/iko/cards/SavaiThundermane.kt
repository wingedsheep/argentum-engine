package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Savai Thundermane
 * {R}{W}
 * Creature — Elemental Cat
 * 3/2
 * Whenever you cycle a card, you may pay {2}. When you do, this creature deals 2 damage to target
 * creature and you gain 2 life.
 *
 * Escape Protocol's shape: the optional {2} payment is the reflexive action, and the target is chosen
 * only when the payment is made.
 */
val SavaiThundermane = card("Savai Thundermane") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Elemental Cat"
    power = 3
    toughness = 2
    oracleText = "Whenever you cycle a card, you may pay {2}. When you do, this creature deals 2 damage " +
        "to target creature and you gain 2 life."

    triggeredAbility {
        trigger = Triggers.you.cycles()
        effect = Effects.ReflexiveTrigger(
            // "you may pay {2}"
            action = Effects.PayMana("{2}"),
            optional = true) {
            // "When you do, this creature deals 2 damage to target creature and you gain 2 life."
            val creature = target(TargetFilter.Creature)
            effect = Effects.DealDamage(2, creature) then Effects.GainLife(2)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Svetlin Velinov"
        flavorText = "A crackling roar is the last sound many hunters hear."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73d3925c-cf7d-4546-bc70-33f04d4b7566.jpg?1783931018"
    }
}
