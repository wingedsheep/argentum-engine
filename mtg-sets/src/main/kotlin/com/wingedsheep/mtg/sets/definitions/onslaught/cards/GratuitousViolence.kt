package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Gratuitous Violence
 * {2}{R}{R}{R}
 * Enchantment
 * If a creature you control would deal damage to a permanent or player,
 * it deals double that damage instead.
 */
val GratuitousViolence = card("Gratuitous Violence") {
    manaCost = "{2}{R}{R}{R}"
    typeLine = "Enchantment"
    oracleText = "If a creature you control would deal damage to a permanent or player, it deals double that damage instead."

    replacementEffect(
        DoubleDamage(
            appliesTo = GameEvent.DamageEvent(
                source = SourceFilter.Matching(GameObjectFilter.Creature.youControl()),
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/large/front/4/b/4b0c5d14-4fab-4034-a2d3-0d851ef67cbd.jpg?1562912596"
    }
}
