package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Executioner's Swing
 * {W}{B}
 * Instant
 * Target creature that dealt damage this turn gets -5/-5 until end of turn.
 *
 * The target filter is the active voice — `hasDealtDamageThisTurn()` — so combat and noncombat
 * damage to any recipient both qualify, per the ruling.
 */
val ExecutionersSwing = card("Executioner's Swing") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Target creature that dealt damage this turn gets -5/-5 until end of turn."

    spell {
        val creature = target(TargetFilter.Creature.hasDealtDamageThisTurn())
        effect = Effects.ModifyStats(-5, -5, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Karl Kopinski"
        flavorText = "\"The contract specified an appendage for a missed payment. Read the fine print: the head is an appendage.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/1/2122586d-9b23-47c2-8b00-e673aa0310f0.jpg?1783940108"
        ruling(
            "2013-01-24",
            "You can't choose a creature that hasn't yet dealt damage during the turn as the target of Executioner's Swing, so in most cases it's not possible for Executioner's Swing to reduce the amount of combat damage a creature deals. (If a creature has double strike, you could cast Executioner's Swing after it deals first-strike damage but before it deals regular combat damage.)"
        )
        ruling("2013-01-24", "It doesn't matter what the creature dealt damage to or if that damage was combat damage.")
    }
}
