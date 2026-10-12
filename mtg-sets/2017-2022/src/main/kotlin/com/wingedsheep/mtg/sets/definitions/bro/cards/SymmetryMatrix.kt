package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Symmetry Matrix
 * {4}
 * Artifact
 * Whenever a creature you control with power equal to its toughness enters, you may pay {1}.
 * If you do, draw a card.
 *
 * "With power equal to its toughness" is part of the trigger event, so it is checked once, when
 * the creature enters (CR 603.2) — a `triggerRestriction` comparing the entering creature's
 * power and toughness, not an intervening-"if" that would be re-checked on resolution. The
 * optional payment is Dawn of Hope's [Effects.MayPay] mana form.
 */
val SymmetryMatrix = card("Symmetry Matrix") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "Whenever a creature you control with power equal to its toughness enters, you may pay {1}. " +
        "If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.triggeringPower(),
            ComparisonOperator.EQ,
            DynamicAmounts.triggeringToughness()
        )
        effect = Effects.MayPay(ManaCost.parse("{1}"), Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "Slawomir Maniak"
        flavorText = "\"Achieving perfection is simple. Watch, I'll do it twice.\"\n—Mishra"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/786bce65-1c04-45fc-b81b-e7590afbd117.jpg?1783920011"
    }
}
