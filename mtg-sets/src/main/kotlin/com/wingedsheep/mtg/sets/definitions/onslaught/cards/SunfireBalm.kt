package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.PreventNextDamageEffect
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Sunfire Balm
 * {2}{W}
 * Instant
 * Prevent the next 4 damage that would be dealt to any target this turn.
 * Cycling {1}{W}
 * When you cycle Sunfire Balm, you may prevent the next 1 damage that would be dealt to any target this turn.
 */
val SunfireBalm = card("Sunfire Balm") {
    manaCost = "{2}{W}"
    typeLine = "Instant"
    oracleText = "Prevent the next 4 damage that would be dealt to any target this turn.\nCycling {1}{W}\nWhen you cycle Sunfire Balm, you may prevent the next 1 damage that would be dealt to any target this turn."

    spell {
        val t = target("target", Targets.Any)
        effect = PreventNextDamageEffect(
            amount = DynamicAmount.Fixed(4),
            target = t
        )
    }

    keywordAbility(KeywordAbility.cycling("{1}{W}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        val t = target("target", Targets.Any)
        effect = MayEffect(
            PreventNextDamageEffect(
                amount = DynamicAmount.Fixed(1),
                target = t
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Monte Michael Moore"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d563ebb-ecd1-406c-9d69-c101acdeced7.jpg?1562898076"
    }
}
