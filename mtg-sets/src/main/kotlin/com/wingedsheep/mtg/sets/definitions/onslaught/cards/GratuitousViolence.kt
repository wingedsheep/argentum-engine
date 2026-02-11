package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SourceFilter

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
        imageUri = "https://cards.scryfall.io/normal/front/0/9/093e3fc5-b2e0-4376-b8ad-4470e02571c7.jpg?1562896787"
    }
}
