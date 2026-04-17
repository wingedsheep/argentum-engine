package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Thoughtweft Charge
 * {1}{G}
 * Instant
 *
 * Target creature gets +3/+3 until end of turn. If a creature entered the
 * battlefield under your control this turn, draw a card.
 */
val ThoughtweftCharge = card("Thoughtweft Charge") {
    manaCost = "{1}{G}"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+3 until end of turn. " +
        "If a creature entered the battlefield under your control this turn, draw a card."

    spell {
        val creature = target("creature", Targets.Creature)

        effect = Effects.ModifyStats(3, 3, creature)
            .then(ConditionalEffect(
                condition = Exists(
                    Player.You,
                    Zone.BATTLEFIELD,
                    GameObjectFilter.Creature.youControl().enteredThisTurn()
                ),
                effect = Effects.DrawCards(1)
            ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Josiah \"Jo\" Cameron"
        flavorText = "United in mind, soul, and muscle."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19cfc015-b6d5-4918-99a7-990209e77441.jpg?1767872036"
    }
}
