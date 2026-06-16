package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Deepmuck Desperado
 * {2}{U}
 * Creature — Homarid Mercenary
 * 2/4
 * Whenever you commit a crime, each opponent mills three cards. This ability triggers only once each turn.
 *
 * Crime trigger ([Triggers.YouCommitCrime]) capped with `oncePerTurn = true`. The mill is fanned out
 * over [Player.EachOpponent] via [ForEachPlayerEffect] wrapping the standard mill pipeline.
 */
val DeepmuckDesperado = card("Deepmuck Desperado") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Homarid Mercenary"
    power = 2
    toughness = 4
    oracleText = "Whenever you commit a crime, each opponent mills three cards. This ability triggers only once each turn. " +
        "(Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)"

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = ForEachPlayerEffect(
            players = Player.EachOpponent,
            effects = Patterns.Library.mill(3).effects
        )
        description = "Whenever you commit a crime, each opponent mills three cards. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Loïc Canavaggia"
        flavorText = "\"Heard you've been horning in on my turf. How about I introduce you to some surf?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f95ee726-7465-40f8-a954-19b19e636c12.jpg?1712355396"
    }
}
