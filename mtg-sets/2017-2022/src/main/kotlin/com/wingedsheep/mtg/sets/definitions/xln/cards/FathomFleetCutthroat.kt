package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fathom Fleet Cutthroat (XLN #107)
 * {3}{B}  Creature — Human Pirate  3/3
 *
 * When this creature enters, destroy target creature an opponent controls that was dealt damage this turn.
 */
val FathomFleetCutthroat = card("Fathom Fleet Cutthroat") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    oracleText = "When this creature enters, destroy target creature an opponent controls that was dealt damage this turn."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.opponentControls().wasDealtDamageThisTurn())
        )
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Josu Hernaiz"
        flavorText = "\"It was a bad evening for you, mate. Now it's chow time for the sharks.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e83500ed-8c46-488e-bd8e-197bedb8f4bd.jpg"
    }
}
