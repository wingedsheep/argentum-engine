package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.targeting.AnyTarget

/**
 * Spark Spray
 * {R}
 * Instant
 * Spark Spray deals 1 damage to any target.
 * Cycling {R}
 */
val SparkSpray = card("Spark Spray") {
    manaCost = "{R}"
    typeLine = "Instant"
    oracleText = "Spark Spray deals 1 damage to any target.\nCycling {R}"

    spell {
        target = AnyTarget()
        effect = DealDamageEffect(1, EffectTarget.ContextTarget(0))
    }

    keywordAbility(KeywordAbility.cycling("{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Pete Venters"
        flavorText = "It's the only kind of shower goblins will tolerate."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f60d8716-4297-484c-8e02-c30ce2773a65.jpg?1562536945"
    }
}
