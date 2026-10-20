package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * In the Eye of Chaos
 * {2}{U}
 * World Enchantment
 *
 * Whenever a player casts an instant spell, counter it unless that player pays {X}, where X is
 * its mana value.
 *
 * The cast trigger taxes the spell that fired it (`CounterTargetSource.TriggeringEntity`); X is
 * read off that spell as the ability resolves, so an X spell pays for its announced X too.
 */
val InTheEyeOfChaos = card("In the Eye of Chaos") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "World Enchantment"
    oracleText = "Whenever a player casts an instant spell, counter it unless that player pays {X}, " +
        "where X is its mana value."

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Instant)
        effect = Effects.CounterUnlessDynamicPays(
            amount = DynamicAmounts.manaValueOf(EffectTarget.TriggeringEntity),
            targetSource = CounterTargetSource.TriggeringEntity
        )
        description = "Whenever a player casts an instant spell, counter it unless that player pays {X}, " +
            "where X is its mana value."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/733933dd-c871-4f75-8b08-d7c010dddbe6.jpg?1783948075"
    }
}
