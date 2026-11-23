package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Annihilating Fire
 * {1}{R}{R}
 * Instant
 * Annihilating Fire deals 3 damage to any target. If a creature dealt damage this way would die
 * this turn, exile it instead.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Pillar of Flame's shape: the [Effects.MarkExileOnDeath] mark goes on before the damage so a
 * creature killed by this very spell is exiled. Marking a player is a no-op in the executor.
 */
val AnnihilatingFire = card("Annihilating Fire") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Annihilating Fire deals 3 damage to any target. If a creature dealt damage this way " +
        "would die this turn, exile it instead."

    spell {
        val t = target(Targets.Any)
        effect = Effects.MarkExileOnDeath(t) then Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Clint Cearley"
        flavorText = "\"The most impressive performances can be done only once.\"\n—Mote, Rakdos madcap"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae12fd10-c13e-4777-a233-96204ec75ac1.jpg?1783940358"
    }
}
