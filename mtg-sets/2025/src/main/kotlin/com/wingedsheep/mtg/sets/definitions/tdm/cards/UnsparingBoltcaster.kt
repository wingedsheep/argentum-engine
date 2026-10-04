package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unsparing Boltcaster — Tarkir: Dragonstorm #130
 * {2}{R}
 * Creature — Ogre Wizard
 * 3/3
 *
 * When this creature enters, it deals 5 damage to target creature an opponent
 * controls that was dealt damage this turn.
 *
 * The target is constrained to creatures an opponent controls that already took
 * damage this turn via `StatePredicate.WasDealtDamageThisTurn`. If no such
 * creature exists, the trigger has no legal target and is removed from the stack.
 */
val UnsparingBoltcaster = card("Unsparing Boltcaster") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Wizard"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, it deals 5 damage to target creature an opponent " +
        "controls that was dealt damage this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.opponentControls().wasDealtDamageThisTurn())
        effect = Effects.DealDamage(5, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/204f5e5e-d87f-4aee-84e3-28afe8e21591.jpg?1743204485"
    }
}
