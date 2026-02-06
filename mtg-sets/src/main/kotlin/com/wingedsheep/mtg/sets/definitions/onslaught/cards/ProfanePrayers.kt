package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.targeting.AnyTarget

/**
 * Profane Prayers
 * {2}{B}{B}
 * Sorcery
 * Profane Prayers deals X damage to any target and you gain X life,
 * where X is the number of Clerics on the battlefield.
 */
val ProfanePrayers = card("Profane Prayers") {
    manaCost = "{2}{B}{B}"
    typeLine = "Sorcery"

    spell {
        target = AnyTarget()
        effect = DealDamageEffect(
            DynamicAmounts.creaturesWithSubtype(Subtype("Cleric")),
            EffectTarget.ContextTarget(0)
        ) then GainLifeEffect(
            DynamicAmounts.creaturesWithSubtype(Subtype("Cleric"))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Matthew D. Wilson"
        flavorText = "\"Night after night the enemy squanders its dead on our swords. The Cabal does not squander its dead.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2b3e3e8-14a8-495a-97a4-eed4b06bef2e.jpg?1562943564"
    }
}
