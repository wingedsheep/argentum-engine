package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Status // Statue (GRN 230) — split-layout spell (CR 709).
 *
 * Status {B/G} — Instant
 *   Target creature gets +1/+1 and gains deathtouch until end of turn.
 *
 * Statue {2}{B}{G} — Instant
 *   Destroy target artifact, creature, or enchantment.
 */
val StatusStatue = card("Status // Statue") {
    layout = CardLayout.SPLIT
    colorIdentity = "BG"

    face("Status") {
        manaCost = "{B/G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +1/+1 and gains deathtouch until end of turn."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(1, 1, creature) then
                Effects.GrantKeyword(Keyword.DEATHTOUCH, creature)
        }
    }

    face("Statue") {
        manaCost = "{2}{B}{G}"
        typeLine = "Instant"
        oracleText = "Destroy target artifact, creature, or enchantment."

        spell {
            val permanent = target(TargetFilter.ArtifactCreatureOrEnchantment)
            effect = Effects.Destroy(permanent)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Ben Wootten"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44614c6d-5508-4077-b825-66d5d684086c.jpg?1783934111"
    }
}
