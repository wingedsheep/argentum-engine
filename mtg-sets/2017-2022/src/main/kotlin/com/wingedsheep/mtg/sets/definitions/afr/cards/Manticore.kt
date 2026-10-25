package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Manticore
 * {3}{B}
 * Creature — Manticore
 * 2/1
 * Flash
 * Flying
 * Tail Spikes — When this creature enters, destroy target creature an opponent controls that was
 * dealt damage this turn.
 */
val Manticore = card("Manticore") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Manticore"
    power = 2
    toughness = 1
    oracleText = "Flash\nFlying\nTail Spikes — When this creature enters, destroy target creature an " +
        "opponent controls that was dealt damage this turn."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.opponentControls().wasDealtDamageThisTurn())
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b3d9117-724a-4b67-be27-142b9c283788.jpg?1783926491"
    }
}
