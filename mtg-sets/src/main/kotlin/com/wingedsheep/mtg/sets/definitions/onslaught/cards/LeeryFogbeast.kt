package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PreventAllCombatDamageThisTurnEffect

/**
 * Leery Fogbeast
 * {2}{G}
 * Creature — Beast
 * 4/2
 * Whenever Leery Fogbeast becomes blocked, prevent all combat damage that would be dealt this turn.
 */
val LeeryFogbeast = card("Leery Fogbeast") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 2

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = PreventAllCombatDamageThisTurnEffect
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "273"
        artist = "Matt Cavotta"
        flavorText = "It emerges from the mists only to feed."
        imageUri = "https://cards.scryfall.io/large/front/0/0/003dc550-3f14-4267-95f6-3c94db4e51ad.jpg?1562895001"
    }
}
