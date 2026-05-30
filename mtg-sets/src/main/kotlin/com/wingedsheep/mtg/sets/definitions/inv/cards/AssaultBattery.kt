package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Assault // Battery (INV 295) — split-layout spell (CR 709).
 *
 * Assault {R} — Sorcery
 *   Assault deals 2 damage to any target.
 *
 * Battery {3}{G} — Sorcery
 *   Create a 3/3 green Elephant creature token.
 *
 * Cast either half from hand; only the chosen half goes on the stack (CR 709.4).
 */
val AssaultBattery = card("Assault // Battery") {
    layout = CardLayout.SPLIT
    colorIdentity = "RG"

    face("Assault") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        oracleText = "Assault deals 2 damage to any target."

        spell {
            target("any target", Targets.Any)
            effect = Effects.DealDamage(2, EffectTarget.ContextTarget(0))
        }
    }

    face("Battery") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery"
        oracleText = "Create a 3/3 green Elephant creature token."

        spell {
            effect = Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Elephant"),
                imageUri = "https://cards.scryfall.io/normal/front/3/d/3dc13cf6-665a-4d92-836f-b2e10ca08ecd.jpg?1561756962"
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "295"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ec6a889-c941-4898-a2f6-4d3863faf535.jpg?1562898037"
    }
}
