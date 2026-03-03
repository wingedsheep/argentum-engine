package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Force Away
 * {1}{U}
 * Instant
 * Return target creature to its owner's hand.
 * Ferocious — If you control a creature with power 4 or greater,
 * you may draw a card. If you do, discard a card.
 */
val ForceAway = card("Force Away") {
    manaCost = "{1}{U}"
    typeLine = "Instant"
    oracleText = "Return target creature to its owner's hand.\nFerocious — If you control a creature with power 4 or greater, you may draw a card. If you do, discard a card."

    spell {
        val creature = target("creature", Targets.Creature)

        effect = Effects.ReturnToHand(creature)
            .then(ConditionalEffect(
                condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
                effect = MayEffect(Effects.Loot())
            ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Mark Winters"
        flavorText = "\"Where an enemy once rode, not even a whisper remains.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dda70b3e-4b70-404e-a579-41dd126be084.jpg?1562794644"
    }
}
