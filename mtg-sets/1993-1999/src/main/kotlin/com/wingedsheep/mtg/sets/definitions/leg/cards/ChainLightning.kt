package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CopyRecipient

/**
 * Chain Lightning
 * {R}
 * Sorcery
 * Chain Lightning deals 3 damage to any target. Then that player or that permanent's controller
 * may pay {R}{R}. If the player does, they may copy this spell and may choose a new target for
 * that copy.
 */
val ChainLightning = card("Chain Lightning") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Chain Lightning deals 3 damage to any target. Then that player or that permanent's controller may pay {R}{R}. If the player does, they may copy this spell and may choose a new target for that copy."

    spell {
        val t = target(Targets.Any)
        effect = Effects.ChainCopy(
            action = Effects.DealDamage(3, t),
            target = t,
            offerTo = CopyRecipient.AFFECTED_PLAYER,
            copyTarget = Targets.Any,
            copyCost = Costs.pay.Mana("{R}{R}")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5883762-ca0a-4932-8d2a-41a45796a5f8.jpg?1783948058"
    }
}
