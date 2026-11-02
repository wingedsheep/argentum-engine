package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Integrity // Intervention (GRN 227) — split-layout spell (CR 709).
 *
 * Integrity {R/W} — Instant
 *   Target creature gets +2/+2 until end of turn.
 *
 * Intervention {2}{R}{W} — Instant
 *   Intervention deals 3 damage to any target and you gain 3 life.
 */
val IntegrityIntervention = card("Integrity // Intervention") {
    layout = CardLayout.SPLIT
    colorIdentity = "RW"

    face("Integrity") {
        manaCost = "{R/W}"
        typeLine = "Instant"
        oracleText = "Target creature gets +2/+2 until end of turn."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 2, creature)
        }
    }

    face("Intervention") {
        manaCost = "{2}{R}{W}"
        typeLine = "Instant"
        oracleText = "Intervention deals 3 damage to any target and you gain 3 life."

        spell {
            val any = target(Targets.Any)
            effect = Effects.DealDamage(3, any) then Effects.GainLife(3)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Ben Maier"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a82084e-b178-442b-8007-7b2a70f3fbba.jpg?1783934111"
    }
}
