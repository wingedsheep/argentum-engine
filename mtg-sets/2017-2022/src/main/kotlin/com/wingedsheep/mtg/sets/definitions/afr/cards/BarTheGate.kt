package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bar the Gate
 * {2}{U}
 * Instant
 *
 * Counter target creature or planeswalker spell. Venture into the dungeon. (Enter the first room
 * or advance to the next room.)
 *
 * Launch Mishap's shape with a venture in place of the Thopter: the spell has a single target, so
 * if that target is gone on resolution the whole spell doesn't resolve and there is no venture.
 */
val BarTheGate = card("Bar the Gate") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature or planeswalker spell. Venture into the dungeon. " +
        "(Enter the first room or advance to the next room.)"

    spell {
        target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker, zone = Zone.STACK))
        effect = Effects.CounterSpell() then Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Johannes Voss"
        flavorText = "\"This path is closed to you!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9b1e53f-1384-4860-9944-e68922afc65c.jpg?1783926518"
    }
}
