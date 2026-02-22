package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Renewed Faith
 * {2}{W}
 * Instant
 * You gain 6 life.
 * Cycling {1}{W}
 * When you cycle Renewed Faith, you may gain 2 life.
 */
val RenewedFaith = card("Renewed Faith") {
    manaCost = "{2}{W}"
    typeLine = "Instant"
    oracleText = "You gain 6 life.\nCycling {1}{W}\nWhen you cycle Renewed Faith, you may gain 2 life."

    spell {
        effect = GainLifeEffect(6)
    }

    keywordAbility(KeywordAbility.cycling("{1}{W}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        effect = MayEffect(GainLifeEffect(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Dave Dorman"
        flavorText = "Sometimes the clearest road to renewal is through utter devastation."
        imageUri = "https://cards.scryfall.io/large/front/1/e/1ea572b5-ff68-45aa-8200-78ee7f64a0ce.jpg?1562902188"
    }
}
