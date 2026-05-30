package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stand // Deliver (INV 292) — split-layout spell (CR 709).
 *
 * Stand {W} — Instant
 *   Prevent the next 2 damage that would be dealt to target creature this turn.
 *
 * Deliver {2}{U} — Instant
 *   Return target permanent to its owner's hand.
 *
 * Cast either half from hand; only the chosen half goes on the stack (CR 709.4).
 */
val StandDeliver = card("Stand // Deliver") {
    layout = CardLayout.SPLIT
    colorIdentity = "WU"

    face("Stand") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Prevent the next 2 damage that would be dealt to target creature this turn."

        spell {
            target("target creature", Targets.Creature)
            effect = Effects.PreventNextDamage(2, EffectTarget.ContextTarget(0))
        }
    }

    face("Deliver") {
        manaCost = "{2}{U}"
        typeLine = "Instant"
        oracleText = "Return target permanent to its owner's hand."

        spell {
            target("target permanent", Targets.Permanent)
            effect = Effects.ReturnToHand(EffectTarget.ContextTarget(0))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "292"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be8b338f-6f05-43c6-beeb-c5052cc0d6a9.jpg?1562933317"
    }
}
