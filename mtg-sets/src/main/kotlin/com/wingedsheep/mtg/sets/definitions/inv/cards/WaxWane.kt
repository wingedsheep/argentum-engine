package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wax // Wane (INV 296) — split-layout spell (CR 709).
 *
 * Wax {G} — Instant
 *   Target creature gets +2/+2 until end of turn.
 *
 * Wane {W} — Instant
 *   Destroy target enchantment.
 *
 * Cast either half from hand; only the chosen half goes on the stack (CR 709.4).
 */
val WaxWane = card("Wax // Wane") {
    layout = CardLayout.SPLIT
    colorIdentity = "GW"

    face("Wax") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +2/+2 until end of turn."

        spell {
            target("target creature", Targets.Creature)
            effect = Effects.ModifyStats(2, 2, EffectTarget.ContextTarget(0))
        }
    }

    face("Wane") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Destroy target enchantment."

        spell {
            target("target enchantment", Targets.Enchantment)
            effect = Effects.Destroy(EffectTarget.ContextTarget(0))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "296"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19859061-f5ec-4b7f-86a1-196f98648e0a.jpg?1562900084"
    }
}
