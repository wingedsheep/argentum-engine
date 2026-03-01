package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Feed the Clan
 * {1}{G}
 * Instant
 * You gain 5 life.
 * Ferocious — You gain 10 life instead if you control a creature with power 4 or greater.
 */
val FeedTheClan = card("Feed the Clan") {
    manaCost = "{1}{G}"
    typeLine = "Instant"
    oracleText = "You gain 5 life.\nFerocious — You gain 10 life instead if you control a creature with power 4 or greater."

    spell {
        // Ferocious: if you control a creature with power 4+, gain 10 life instead of 5
        effect = ConditionalEffect(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
            effect = Effects.GainLife(10),
            elseEffect = Effects.GainLife(5)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Winona Nelson"
        flavorText = "The Temur believe three things only are needed in life: a hot fire, a full belly, and a strong companion."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52f7c53d-0b53-400f-aa67-967547f3e394.jpg?1562786646"
    }
}
