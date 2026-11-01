package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bedeck // Bedazzle (RNA 221) — split-layout spell (CR 709).
 *
 * Bedeck {B/R}{B/R} — Instant
 *   Target creature gets +3/-3 until end of turn.
 *
 * Bedazzle {4}{B}{R} — Instant
 *   Destroy target nonbasic land. Bedazzle deals 2 damage to target opponent or planeswalker.
 *
 * Bedazzle needs both targets to be cast; if one becomes illegal, the other is still affected.
 */
val BedeckBedazzle = card("Bedeck // Bedazzle") {
    layout = CardLayout.SPLIT
    colorIdentity = "BR"

    face("Bedeck") {
        manaCost = "{B/R}{B/R}"
        typeLine = "Instant"
        oracleText = "Target creature gets +3/-3 until end of turn."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(3, -3, creature)
        }
    }

    face("Bedazzle") {
        manaCost = "{4}{B}{R}"
        typeLine = "Instant"
        oracleText = "Destroy target nonbasic land. Bedazzle deals 2 damage to target opponent or planeswalker."

        spell {
            val land = target(TargetFilter.NonbasicLand)
            val victim = target(Targets.OpponentOrPlaneswalker)
            effect = Effects.Destroy(land) then Effects.DealDamage(2, victim)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5873efa-d573-4435-81ad-48df2ca5c7f2.jpg?1783933629"
        ruling(
            "2019-01-25",
            "You can't cast Bedazzle without a target nonbasic land and a target opponent or planeswalker. " +
                "If one of the targets is illegal when Bedazzle tries to resolve, the other is still affected as appropriate."
        )
    }
}
