package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sage-Eye Avengers
 * {4}{U}{U}
 * Creature — Djinn Monk
 * 4/5
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * Whenever this creature attacks, you may return target creature to its owner's hand if its power
 * is less than this creature's power.
 *
 * The power comparison is a resolution-time check, not a targeting restriction: any creature may be
 * targeted, and on resolution the bounce happens only if the target's current power is strictly
 * less than the Avengers' current power (its last-known power if it has left the battlefield).
 * The "may" is asked only when the comparison holds — declining or failing it changes nothing.
 */
val SageEyeAvengers = card("Sage-Eye Avengers") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn Monk"
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "Whenever this creature attacks, you may return target creature to its owner's hand if its power is less than this creature's power."
    power = 4
    toughness = 5

    prowess()

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.powerOf(creature),
                ComparisonOperator.LT,
                DynamicAmounts.sourcePower(),
            ),
            then = Effects.May(Effects.ReturnToHand(creature)),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8050bb72-4fd4-4acd-ab21-e83f8503dcc1.jpg?1783938702"
    }
}
