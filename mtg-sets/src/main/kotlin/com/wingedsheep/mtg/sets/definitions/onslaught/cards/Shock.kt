package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Shock
 * {R}
 * Instant
 * Shock deals 2 damage to any target.
 */
val Shock = card("Shock") {
    manaCost = "{R}"
    typeLine = "Instant"
    oracleText = "Shock deals 2 damage to any target."

    spell {
        target = AnyTarget()
        effect = DealDamageEffect(2, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "227"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"I love lightning! It's my best invention since the rock.\"\n—Toggo, goblin weaponsmith"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83c92b5d-103c-4719-a850-690a7010291a.jpg"
    }
}
