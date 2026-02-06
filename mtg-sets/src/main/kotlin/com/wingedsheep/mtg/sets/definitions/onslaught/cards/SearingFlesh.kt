package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Searing Flesh
 * {6}{R}
 * Sorcery
 * Searing Flesh deals 7 damage to target opponent.
 */
val SearingFlesh = card("Searing Flesh") {
    manaCost = "{6}{R}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = DealDamageEffect(7, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Pete Venters"
        flavorText = "When it comes to their enemies, goblins can be very warm and generous."
        imageUri = "https://cards.scryfall.io/large/front/d/8/d83db110-42e7-4823-a686-b83205faf503.jpg?1562946564"
    }
}
