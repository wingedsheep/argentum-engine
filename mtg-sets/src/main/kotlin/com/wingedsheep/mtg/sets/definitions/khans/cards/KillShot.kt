package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kill Shot
 * {2}{W}
 * Instant
 * Destroy target attacking creature.
 */
val KillShot = card("Kill Shot") {
    manaCost = "{2}{W}"
    typeLine = "Instant"
    oracleText = "Destroy target attacking creature."

    spell {
        val t = target("target", Targets.AttackingCreature)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Michael C. Hayes"
        flavorText = "\"Mardu archers are trained in Dakla, the way of the bow. They never miss their target, no matter how small, how fast, or how far away.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f30d4136-78a3-4760-83af-d365cc97d118.jpg?1562795914"
    }
}
