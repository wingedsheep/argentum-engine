package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dwarven Song
 * {R}
 * Instant
 * One or more target creatures become red until end of turn.
 *
 * "One or more" is an unlimited target count with a floor of one: `unlimited = true, minCount = 1`.
 */
val DwarvenSong = card("Dwarven Song") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "One or more target creatures become red until end of turn."

    spell {
        targets(TargetFilter.Creature, unlimited = true, minCount = 1)
        effect = Effects.ForEachTarget(Effects.ChangeColor(EffectTarget.ContextTarget(0), setOf(Color.RED)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29a50f72-9524-4440-9380-9d3e0b693351.jpg?1783948057"
    }
}
