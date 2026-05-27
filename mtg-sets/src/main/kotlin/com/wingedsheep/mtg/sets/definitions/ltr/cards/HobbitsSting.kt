package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hobbit's Sting
 * {1}{W}
 * Instant
 *
 * Hobbit's Sting deals X damage to target creature, where X is the number of creatures
 * you control plus the number of Foods you control.
 */
val HobbitsSting = card("Hobbit's Sting") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Hobbit's Sting deals X damage to target creature, where X is the number of creatures you control plus the number of Foods you control."

    spell {
        target("target creature", Targets.Creature)
        effect = Effects.DealDamage(
            DynamicAmount.Add(
                DynamicAmount.Count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature),
                DynamicAmount.Count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Any.withSubtype("Food"))
            ),
            EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Viko Menezes"
        flavorText = "\"One for the Shire!\" cried Aragorn. \"The Hobbit's bite is deep! You have a good blade, Frodo son of Drogo!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/1/019eab42-9e0c-4958-ac97-74d3db5580f3.jpg?1686967829"
    }
}
