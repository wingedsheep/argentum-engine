package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blood Price
 * {3}{B}
 * Sorcery
 * Look at the top four cards of your library. Put two of them into your hand and the rest on the
 * bottom of your library in any order. You lose 2 life.
 *
 * [Patterns.Library.lookAtTopAndKeep] (keep two to hand, rest to the bottom in the controller's
 * order), followed by [Effects.LoseLife] on the controller.
 */
val BloodPrice = card("Blood Price") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Look at the top four cards of your library. Put two of them into your hand and the rest on the bottom of your library in any order. You lose 2 life."

    spell {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 4,
            keepCount = 2,
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        ) then Effects.LoseLife(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Antonio José Manzanedo"
        flavorText = "Blood is the oldest currency."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df14d32d-fe53-4370-8298-fab528ff12db.jpg?1783929381"
    }
}
