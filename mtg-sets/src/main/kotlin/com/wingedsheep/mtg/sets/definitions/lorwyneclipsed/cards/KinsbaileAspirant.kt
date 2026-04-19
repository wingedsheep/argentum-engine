package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kinsbaile Aspirant
 * {W}
 * Creature — Kithkin Citizen
 * 2/1
 *
 * As an additional cost to cast this spell, behold a Kithkin or pay {2}.
 * (To behold a Kithkin, choose a Kithkin you control or reveal a Kithkin card from your hand.)
 * Whenever another creature you control enters, this creature gets +1/+1 until end of turn.
 */
val KinsbaileAspirant = card("Kinsbaile Aspirant") {
    manaCost = "{W}"
    typeLine = "Creature — Kithkin Citizen"
    power = 2
    toughness = 1
    oracleText = "As an additional cost to cast this spell, behold a Kithkin or pay {2}. " +
        "(To behold a Kithkin, choose a Kithkin you control or reveal a Kithkin card from your hand.)\n" +
        "Whenever another creature you control enters, this creature gets +1/+1 until end of turn."

    additionalCost(
        AdditionalCost.BeholdOrPay(
            filter = Filters.WithSubtype("Kithkin"),
            alternativeManaCost = "{2}"
        )
    )

    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        effect = ModifyStatsEffect(1, 1, EffectTarget.Self, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Margaret Organ-Kean"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56dfdab1-ea3f-4663-a855-a9e72505f85e.jpg?1767732481"
    }
}
