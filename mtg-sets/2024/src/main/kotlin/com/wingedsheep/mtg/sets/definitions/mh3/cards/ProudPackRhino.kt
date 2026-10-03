package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Proud Pack-Rhino
 * {2}{W}
 * Creature — Rhino
 * 3/3
 *
 * When this creature enters, choose one —
 * • Put a shield counter on target permanent.
 * • Proliferate.
 */
val ProudPackRhino = card("Proud Pack-Rhino") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rhino"
    oracleText = "When this creature enters, choose one —\n" +
        "• Put a shield counter on target permanent. (If it would be dealt damage or destroyed, remove a shield counter from it instead.)\n" +
        "• Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Put a shield counter on target permanent") {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.AddCounters(CounterType.SHIELD, 1, permanent)
            },
            mode("Proliferate") {
                effect = Effects.Proliferate()
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0821e622-88b8-4b82-a696-954da1dde915.jpg?1783911297"
    }
}
