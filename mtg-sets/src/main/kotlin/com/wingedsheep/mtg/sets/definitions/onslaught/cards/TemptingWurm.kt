package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tempting Wurm
 * {1}{G}
 * Creature — Wurm
 * 5/5
 * When Tempting Wurm enters the battlefield, each opponent may put any number of
 * artifact, creature, enchantment, and/or land cards from their hand onto the battlefield.
 */
val TemptingWurm = card("Tempting Wurm") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Wurm"
    power = 5
    toughness = 5
    oracleText = "When Tempting Wurm enters the battlefield, each opponent may put any number of artifact, creature, enchantment, and/or land cards from their hand onto the battlefield."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.eachOpponentMayPutFromHand(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Enchantment or GameObjectFilter.Land
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "291"
        artist = "Kev Walker"
        flavorText = "Its aroma is as alluring as its diet is voracious."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/857c2b6c-cfdf-4c88-a334-2937cb7db603.jpg?1562926442"
    }
}
