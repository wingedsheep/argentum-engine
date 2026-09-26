package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Initiate of Blood // Goka the Unjust (Champions of Kamigawa #173) — a flip card (CR 710).
 *
 * Initiate of Blood {3}{R} — Creature — Ogre Shaman 2/2
 * "{T}: This creature deals 1 damage to target creature that was dealt damage this turn. When that
 * creature dies this turn, flip this creature."
 *
 * Goka the Unjust — Legendary Creature — Ogre Shaman 4/4
 * "{T}: Goka deals 4 damage to target creature that was dealt damage this turn."
 *
 * The "when that creature dies this turn" rider is a watched-entity delayed trigger created on
 * resolution (the Sandals of Abdallah / Grim Javelineer shape): it fires on that object dying from
 * any cause for the rest of the turn, not only from the Initiate's own damage.
 */
private val InitiateOfBloodUpright = card("Initiate of Blood") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Shaman"
    oracleText = "{T}: This creature deals 1 damage to target creature that was dealt damage this turn. " +
        "When that creature dies this turn, flip this creature."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature.wasDealtDamageThisTurn())
        effect = Effects.DealDamage(1, creature) then
            Effects.CreateDelayedTrigger(
                effect = Effects.Flip(),
                trigger = Triggers.self.dies(),
                watchedTarget = creature,
                expiry = DelayedTriggerExpiry.EndOfTurn,
            )
        description = "{T}: This creature deals 1 damage to target creature that was dealt damage this " +
            "turn. When that creature dies this turn, flip this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3523b8e-065f-427c-8d5b-eb731ca91ede.jpg?1783944299"
    }
}

private val GokaTheUnjust = card("Goka the Unjust") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Ogre Shaman"
    oracleText = "{T}: Goka deals 4 damage to target creature that was dealt damage this turn."
    power = 4
    toughness = 4

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature.wasDealtDamageThisTurn())
        effect = Effects.DealDamage(4, creature)
        description = "{T}: Goka deals 4 damage to target creature that was dealt damage this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3523b8e-065f-427c-8d5b-eb731ca91ede.jpg?1783944299"
    }
}

val InitiateOfBlood: CardDefinition = CardDefinition.flipCard(
    unflipped = InitiateOfBloodUpright,
    flipped = GokaTheUnjust,
)
