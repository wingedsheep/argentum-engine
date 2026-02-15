package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Meddle
 * {1}{U}
 * Instant
 * If target spell has only one target and that target is a creature, change that spell's target to another creature.
 */
val Meddle = card("Meddle") {
    manaCost = "{1}{U}"
    typeLine = "Instant"
    oracleText = "If target spell has only one target and that target is a creature, change that spell's target to another creature."

    spell {
        target = Targets.Spell
        effect = Effects.ChangeSpellTarget()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/685edfe8-9770-47c6-95fb-0816f3126f04.jpg?1562919580"
    }
}
