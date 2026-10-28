package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Constable Odo
 * {3}{W/U}{W/U}
 * Legendary Creature — Shapeshifter Detective
 * 4/3
 *
 * When Constable Odo enters, choose one —
 * • Tap target creature an opponent controls. Put two stun counters on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)
 * • Investigate twice. (To investigate, create a Clue token. It's an artifact with "{2}, Sacrifice this token: Draw a card.")
 */
val ConstableOdo = card("Constable Odo") {
    manaCost = "{3}{W/U}{W/U}"
    typeLine = "Legendary Creature — Shapeshifter Detective"
    colorIdentity = "UW"
    oracleText = "When Constable Odo enters, choose one —\n• Tap target creature an opponent controls. Put two stun counters on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)\n• Investigate twice. (To investigate, create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"
    power = 4
    toughness = 3
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(listOf(
            mode("Tap an opponent's creature and put two stun counters on it") {
                val creature = target(TargetFilter.Creature.opponentControls())
                effect = Effects.Tap(creature) then Effects.AddCounters(CounterType.STUN, 2, creature)
            },
            mode("Investigate twice") { effect = Effects.Investigate(2) }
        ))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3df582e4-3bfb-4413-9b39-898f33a5c1fd.jpg?1792508297"
    }
}
