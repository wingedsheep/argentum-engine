package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Towering-Wave Mystic
 * {1}{U}
 * Creature — Merfolk Wizard
 * 2/1
 * Whenever this creature deals damage, target player mills that many cards.
 *
 * The bare `Triggers.self.dealsDamage()` (El-Hajjâj's shape): with no recipient restriction, combat
 * damage dealt to several recipients at once — trample, or several blockers — is one damage event,
 * so it triggers once with "that many" the total (the engine folds it, as for Vessel of the
 * All-Consuming).
 */
val ToweringWaveMystic = card("Towering-Wave Mystic") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature deals damage, target player mills that many cards."

    triggeredAbility {
        trigger = Triggers.self.dealsDamage()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(DynamicAmounts.triggerDamageAmount(), player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Jason Rainville"
        flavorText = "Thassa's favor followed him to the Underworld. Even the waves of Nerono obeyed his will."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c2b4ffc-b1e1-4b16-9a51-1a12f8256a27.jpg?1783931575"
    }
}
