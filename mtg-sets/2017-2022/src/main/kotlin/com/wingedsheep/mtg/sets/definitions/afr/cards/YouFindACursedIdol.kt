package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * You Find a Cursed Idol
 * {1}{G}
 * Sorcery
 *
 * Choose one —
 * • Smash It — Destroy target artifact.
 * • Lift the Curse — Destroy target enchantment.
 * • Steal Its Eyes — Create a Treasure token and venture into the dungeon.
 *
 * A plain choose-one `modal(chooseCount = 1)`: two single-target destroy modes and an untargeted
 * Treasure-then-venture mode.
 */
val YouFindACursedIdol = card("You Find a Cursed Idol") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Smash It — Destroy target artifact.\n" +
        "• Lift the Curse — Destroy target enchantment.\n" +
        "• Steal Its Eyes — Create a Treasure token and venture into the dungeon. " +
        "(Enter the first room or advance to the next room.)"

    spell {
        modal(chooseCount = 1) {
            mode("Smash It — Destroy target artifact.") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            }
            mode("Lift the Curse — Destroy target enchantment.") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            }
            mode(
                "Steal Its Eyes — Create a Treasure token and venture into the dungeon.",
                Effects.CreateTreasure(1) then Effects.VentureIntoTheDungeon(),
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "213"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efb5671a-942a-4730-9756-32fe600e0528.jpg?1783926452"
    }
}
