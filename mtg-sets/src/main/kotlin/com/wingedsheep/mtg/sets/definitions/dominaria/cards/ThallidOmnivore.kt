package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thallid Omnivore
 * {3}{B}
 * Creature — Fungus
 * 3/3
 * {1}, Sacrifice another creature: Thallid Omnivore gets +2/+2 until end of turn.
 * If a Saproling was sacrificed this way, you gain 2 life.
 */
val ThallidOmnivore = card("Thallid Omnivore") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Fungus"
    power = 3
    toughness = 3
    oracleText = "{1}, Sacrifice another creature: Thallid Omnivore gets +2/+2 until end of turn. If a Saproling was sacrificed this way, you gain 2 life."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
            .then(ConditionalEffect(
                condition = Conditions.SacrificedHadSubtype("Saproling"),
                effect = Effects.GainLife(2)
            ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Mathias Kollros"
        flavorText = "Thelon of Havenwood created thallids as a food source in times of darkness. Thallids did the same with saprolings."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99b35391-f1dc-434b-b581-ca6cb6f3439f.jpg?1562740048"
    }
}
