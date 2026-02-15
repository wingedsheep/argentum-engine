package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DamageAndChainCopyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.AnyTarget

/**
 * Chain of Plasma
 * {1}{R}
 * Instant
 * Chain of Plasma deals 3 damage to any target. Then that player or that permanent's
 * controller may discard a card. If the player does, they may copy this spell and may
 * choose a new target for that copy.
 */
val ChainOfPlasma = card("Chain of Plasma") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Chain of Plasma deals 3 damage to any target. Then that player or that permanent's controller may discard a card. If the player does, they may copy this spell and may choose a new target for that copy."

    spell {
        target = AnyTarget()
        effect = DamageAndChainCopyEffect(
            amount = 3,
            target = EffectTarget.ContextTarget(0),
            spellName = "Chain of Plasma"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Gary Ruddell"
        imageUri = "https://cards.scryfall.io/large/front/a/5/a5655911-daac-4308-a4c4-824e28a29e42.jpg?1562934538"
    }
}
