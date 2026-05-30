package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Restrain
 * {2}{W}
 * Instant
 * Prevent all combat damage that would be dealt by target attacking creature this turn.
 * Draw a card.
 */
val Restrain = card("Restrain") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt by target attacking creature this turn.\n" +
        "Draw a card."

    spell {
        target("target attacking creature", Targets.AttackingCreature)
        effect = Effects.PreventAllDamageDealtBy(EffectTarget.ContextTarget(0)) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Dave Dorman"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6b5c765-619c-4db9-b509-91892fb65e8f.jpg?1562944692"
    }
}
