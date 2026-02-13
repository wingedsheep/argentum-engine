package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateTokenEffect

/**
 * Symbiotic Elf
 * {3}{G}
 * Creature — Elf
 * 2/2
 * When Symbiotic Elf dies, create two 1/1 green Insect creature tokens.
 */
val SymbioticElf = card("Symbiotic Elf") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elf"
    power = 2
    toughness = 2
    oracleText = "When Symbiotic Elf dies, create two 1/1 green Insect creature tokens."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = CreateTokenEffect(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa47df37-f246-4f80-a944-008cdf347dad.jpg?1561757793"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "288"
        artist = "Wayne England"
        flavorText = "The elves arriving in Wirewood had no homes, let alone weapons, but they knew that the forest would always provide."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33af35c6-7802-4366-ad20-1e330b4957ef.jpg?1562907073"
    }
}
