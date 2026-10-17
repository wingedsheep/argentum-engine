package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Call In a Professional
 * {2}{R}
 * Instant
 * Players can't gain life this turn. Damage can't be prevented this turn. Call In a Professional
 * deals 3 damage to any target. (Shield counters don't prevent this damage as they're removed.)
 *
 * - "Players can't gain life this turn" locks life gain for every player through end of turn.
 * - "Damage can't be prevented this turn" sets the turn-scoped flag (Fear, Fire, Foes! shape).
 *   The shield-counter reminder is honoured by the engine's shield handling: the counter is still
 *   removed, but the damage is not prevented while prevention is disabled.
 */
val CallInAProfessional = card("Call In a Professional") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Players can't gain life this turn. Damage can't be prevented this turn. " +
        "Call In a Professional deals 3 damage to any target. " +
        "(Shield counters don't prevent this damage as they're removed.)"

    spell {
        val t = target(Targets.Any)
        effect = Effects.LockLifeGain(EffectTarget.PlayerRef(Player.Each), Duration.EndOfTurn) then
            Effects.DamageCantBePreventedThisTurn() then
            Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Filip Burburan"
        flavorText = "For the next week, Park Heights smelled of burnt oil paint and brimstone."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ead68c0a-eed1-4a9c-a790-56f8a79b444c.jpg?1783923120"
    }
}
