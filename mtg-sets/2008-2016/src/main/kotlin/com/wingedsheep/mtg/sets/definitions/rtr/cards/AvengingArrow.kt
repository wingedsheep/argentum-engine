package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Avenging Arrow
 * {2}{W}
 * Instant
 *
 * Destroy target creature that dealt damage this turn.
 *
 * Canonical printing: Return to Ravnica, the card's only printing.
 *
 * The target filter is the **active** voice — `TargetFilter.hasDealtDamageThisTurn()` (damage the
 * creature dealt, combat or noncombat, to any recipient), not `wasDealtDamageThisTurn()`. The same
 * filter backs Red Guardian, Super-Soldier.
 */
val AvengingArrow = card("Avenging Arrow") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target creature that dealt damage this turn."

    spell {
        val t = target(TargetFilter.Creature.hasDealtDamageThisTurn())
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "James Ryman"
        flavorText = "\"Forgive the theft. Punish the deception.\"\n—Alcarus, Selesnya archer"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/696678ff-44dc-4fe4-bf17-024e86cd0220.jpg?1783940378"
    }
}
