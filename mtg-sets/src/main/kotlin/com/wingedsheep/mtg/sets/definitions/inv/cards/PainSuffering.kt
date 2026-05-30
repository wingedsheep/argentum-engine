package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Pain // Suffering (INV 294) — split-layout spell (CR 709).
 *
 * Pain {B} — Sorcery
 *   Target player discards a card.
 *
 * Suffering {3}{R} — Sorcery
 *   Destroy target land.
 *
 * Cast either half from hand; only the chosen half goes on the stack (CR 709.4).
 */
val PainSuffering = card("Pain // Suffering") {
    layout = CardLayout.SPLIT
    colorIdentity = "BR"

    face("Pain") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Target player discards a card."

        spell {
            val targetPlayer = target("target player", TargetPlayer())
            effect = Effects.Discard(1, targetPlayer)
        }
    }

    face("Suffering") {
        manaCost = "{3}{R}"
        typeLine = "Sorcery"
        oracleText = "Destroy target land."

        spell {
            target("target land", Targets.Land)
            effect = Effects.Destroy(EffectTarget.ContextTarget(0))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "294"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81be27d6-e16f-4158-b2b6-66a0f3315327.jpg?1562921172"
    }
}
