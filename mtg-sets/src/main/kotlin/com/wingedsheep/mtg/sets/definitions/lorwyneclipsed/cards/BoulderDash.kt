package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Boulder Dash
 * {1}{R}
 * Sorcery
 *
 * Boulder Dash deals 2 damage to any target and 1 damage to any other target.
 */
val BoulderDash = card("Boulder Dash") {
    manaCost = "{1}{R}"
    typeLine = "Sorcery"
    oracleText = "Boulder Dash deals 2 damage to any target and 1 damage to any other target."

    spell {
        val first = target("first target", AnyTarget(descriptionOverride = "any target (takes 2 damage)"))
        val second = target("second target", AnyTarget(descriptionOverride = "any other target (takes 1 damage)"))
        effect = Effects.DealDamage(2, first)
            .then(Effects.DealDamage(1, second))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Chuck Lukacs"
        flavorText = "It was the first year the downhill event had been included in the Frogtosser Games. It would also be the last."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/657a2a24-22d1-4bc2-9f22-ed361ae487e3.jpg?1767952124"
    }
}
