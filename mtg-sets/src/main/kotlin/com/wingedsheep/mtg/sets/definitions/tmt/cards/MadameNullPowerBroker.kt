package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Madame Null, Power Broker
 * {2}{B}
 * Legendary Creature — Demon Advisor
 * 1/3
 *
 * Deathtouch
 * Whenever another creature you control enters, you may pay life equal to its
 * power. If you do, put that many +1/+1 counters on it.
 */
val MadameNullPowerBroker = card("Madame Null, Power Broker") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon Advisor"
    oracleText = "Deathtouch\nWhenever another creature you control enters, you may pay life equal to its power. If you do, put that many +1/+1 counters on it."
    power = 1
    toughness = 3

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        // "its power" / "that many" — the entering creature's power, used for both the life
        // payment and the counters (Effects.PayDynamicLife is the dynamic pay-life cost).
        val enteringPower = DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Power)
        effect = OptionalCostEffect(
            cost = Effects.PayDynamicLife(enteringPower),
            ifPaid = Effects.AddDynamicCounters(Counters.PLUS_ONE_PLUS_ONE, enteringPower, EffectTarget.TriggeringEntity)
        )
        description = "Whenever another creature you control enters, you may pay life equal to its power. If you do, put that many +1/+1 counters on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Irina Nordsol"
        flavorText = "\"We suffered a bit of a setback tonight, but rest assured: We'll have the planet purged and primed for you right on schedule.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e712d5c-d6fe-40bb-b8e5-5f50c6ea8d4f.jpg?1769005736"
    }
}
