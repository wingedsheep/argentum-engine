package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Collision // Colossus (RNA 223) — split-layout spell (CR 709).
 *
 * Collision {1}{R/G} — Instant
 *   Collision deals 6 damage to target creature with flying.
 *
 * Colossus {R}{G} — Instant
 *   Target creature gets +4/+2 and gains trample until end of turn.
 */
val CollisionColossus = card("Collision // Colossus") {
    layout = CardLayout.SPLIT
    colorIdentity = "RG"

    face("Collision") {
        manaCost = "{1}{R/G}"
        typeLine = "Instant"
        oracleText = "Collision deals 6 damage to target creature with flying."

        spell {
            val flyer = target(TargetFilter.Creature.withKeyword(Keyword.FLYING))
            effect = Effects.DealDamage(6, flyer)
        }
    }

    face("Colossus") {
        manaCost = "{R}{G}"
        typeLine = "Instant"
        oracleText = "Target creature gets +4/+2 and gains trample until end of turn."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(4, 2, creature) then
                Effects.GrantKeyword(Keyword.TRAMPLE, creature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bd15da6-2b86-4dba-951d-318c7d9a5dde.jpg?1783933628"
    }
}
