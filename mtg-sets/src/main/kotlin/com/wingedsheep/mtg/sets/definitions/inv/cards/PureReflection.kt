package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pure Reflection
 * {2}{W}
 * Enchantment
 * Whenever a player casts a creature spell, destroy all Reflections. Then that player creates
 * an X/X white Reflection creature token, where X is the mana value of that spell.
 */
val PureReflection = card("Pure Reflection") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever a player casts a creature spell, destroy all Reflections. Then that player " +
        "creates an X/X white Reflection creature token, where X is the mana value of that spell."

    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(GameObjectFilter.Creature)
        effect = Effects.Composite(
            Effects.DestroyAll(GameObjectFilter.Creature.withSubtype("Reflection")),
            Effects.CreateDynamicToken(
                dynamicPower = DynamicAmounts.triggeringManaValue(),
                dynamicToughness = DynamicAmounts.triggeringManaValue(),
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Reflection"),
                controller = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbff85a6-a51b-424e-a86b-da52c9b3a9da.jpg?1562932789"
    }
}
