package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateTokenEffect

/**
 * Symbiotic Beast
 * {4}{G}{G}
 * Creature — Insect Beast
 * 4/4
 * When Symbiotic Beast dies, create four 1/1 green Insect creature tokens.
 */
val SymbioticBeast = card("Symbiotic Beast") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Insect Beast"
    power = 4
    toughness = 4
    oracleText = "When Symbiotic Beast dies, create four 1/1 green Insect creature tokens."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = CreateTokenEffect(
            count = 4,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa47df37-f246-4f80-a944-008cdf347dad.jpg?1561757793"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "287"
        artist = "Franz Vohwinkel"
        flavorText = "The insects found a meal in the carrion of the beast's prey. The beast found a spiffy new hairstyle."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb61443d-e47a-4fe1-b777-67a3670a5a56.jpg?1562939214"
    }
}
