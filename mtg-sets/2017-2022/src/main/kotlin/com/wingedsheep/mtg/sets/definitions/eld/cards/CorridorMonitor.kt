package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Corridor Monitor
 * {1}{U}
 * Artifact Creature — Construct
 * 1/4
 *
 * When this creature enters, untap target artifact or creature you control.
 */
val CorridorMonitor = card("Corridor Monitor") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Construct"
    oracleText = "When this creature enters, untap target artifact or creature you control."
    power = 1
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl()))
        effect = Effects.Untap(permanent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Jason Felix"
        flavorText = "Castle Vantress has no locks on its doors, but interlopers rarely make it past the foyer."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9347802a-0971-443c-867a-cb9400f18d5c.jpg?1783932662"
    }
}
