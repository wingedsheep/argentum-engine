package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Warrant // Warden (RNA 230) — split-layout spell (CR 709).
 *
 * Warrant {W/U}{W/U} — Instant
 *   Put target attacking or blocking creature on top of its owner's library.
 *
 * Warden {3}{W}{U} — Sorcery
 *   Create a 4/4 white and blue Sphinx creature token with flying and vigilance.
 */
val WarrantWarden = card("Warrant // Warden") {
    layout = CardLayout.SPLIT
    colorIdentity = "WU"

    face("Warrant") {
        manaCost = "{W/U}{W/U}"
        typeLine = "Instant"
        oracleText = "Put target attacking or blocking creature on top of its owner's library."

        spell {
            val creature = target(TargetFilter.AttackingOrBlockingCreature)
            effect = Effects.PutOnTopOfLibrary(creature)
        }
    }

    face("Warden") {
        manaCost = "{3}{W}{U}"
        typeLine = "Sorcery"
        oracleText = "Create a 4/4 white and blue Sphinx creature token with flying and vigilance."

        spell {
            effect = Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.WHITE, Color.BLUE),
                creatureTypes = setOf("Sphinx"),
                keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
                imageUri = "https://cards.scryfall.io/normal/front/f/8/f82ba894-7b10-45ae-9322-60ef85a2869d.jpg?1783933602",
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0070651d-79aa-4ea6-b703-6ecd3528b548.jpg?1783933626"
        ruling(
            "2019-01-25",
            "An \"attacking creature\" is one that has been declared as an attacker or put onto the battlefield " +
                "attacking during this combat. Unless that creature leaves combat, it continues to be an attacking " +
                "creature through the end of combat step, even if the player it was attacking has left the game or " +
                "the planeswalker it was attacking has left combat. Similarly, a \"blocking creature\" is one that " +
                "has been declared as a blocker or put onto the battlefield blocking during this combat."
        )
    }
}
