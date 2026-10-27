package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Molten Blast — Zendikar Rising #149
 * {2}{R} · Instant
 *
 * Choose one —
 * • Molten Blast deals 2 damage to target creature or planeswalker.
 * • Destroy target artifact.
 */
val MoltenBlast = card("Molten Blast") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Molten Blast deals 2 damage to target creature or planeswalker.\n" +
        "• Destroy target artifact."

    spell {
        modal {
            mode("Molten Blast deals 2 damage to target creature or planeswalker") {
                val victim = target(Targets.CreatureOrPlaneswalker)
                effect = Effects.DealDamage(2, victim)
            }
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Izzy"
        flavorText = "Life among the sudden tremors and violent eruptions of Akoum demands a healthy fear of both."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb1beef2-788b-469b-883b-cf1cca04eeff.jpg"
    }
}
