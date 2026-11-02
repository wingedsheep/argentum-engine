package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ionize
 * {1}{U}{R}
 * Instant
 * Counter target spell. Ionize deals 2 damage to that spell's controller.
 */
val Ionize = card("Ionize") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Counter target spell. Ionize deals 2 damage to that spell's controller."

    spell {
        target(TargetFilter.SpellOnStack)
        // Deal the damage while the spell is still on the stack so `TargetController` can read
        // its controller; countering first moves the spell away and the lookup fails. Both happen
        // in one resolution with nothing in between, so the order is imperceptible to players.
        effect = Effects.DealDamage(2, EffectTarget.TargetController) then Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f161f7d2-eaa1-4931-93f9-befa8b5df821.jpg?1783934131"
    }
}
