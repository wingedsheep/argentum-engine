package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Yamabushi's Flame
 * {2}{R}
 * Instant
 * Yamabushi's Flame deals 3 damage to any target. If a creature dealt damage this way would die
 * this turn, exile it instead.
 *
 * Red Sun's Zenith's shape with a fixed 3, so no X > 0 gate: the [Effects.MarkExileOnDeath] mark goes
 * on *before* the damage so a creature killed by this very spell is exiled. Marking a player is a
 * no-op in the executor.
 */
val YamabushisFlame = card("Yamabushi's Flame") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Yamabushi's Flame deals 3 damage to any target. If a creature dealt damage this way " +
        "would die this turn, exile it instead."

    spell {
        val t = target(Targets.Any)
        effect = Effects.MarkExileOnDeath(t) then Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a9bacba-55c4-4b92-bdd9-01b6035ed1b2.jpg?1783944293"
    }
}
