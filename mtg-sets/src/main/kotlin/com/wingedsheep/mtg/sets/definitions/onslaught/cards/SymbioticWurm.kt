package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateTokenEffect

/**
 * Symbiotic Wurm
 * {5}{G}{G}{G}
 * Creature — Wurm
 * 7/7
 * When Symbiotic Wurm dies, create seven 1/1 green Insect creature tokens.
 */
val SymbioticWurm = card("Symbiotic Wurm") {
    manaCost = "{5}{G}{G}{G}"
    typeLine = "Creature — Wurm"
    power = 7
    toughness = 7

    triggeredAbility {
        trigger = Triggers.Dies
        effect = CreateTokenEffect(
            count = 7,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa47df37-f246-4f80-a944-008cdf347dad.jpg?1561757793"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "289"
        artist = "Matt Cavotta"
        flavorText = "The insects keep the wurm's hide free from parasites. In return, the wurm doesn't eat the insects."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a60313ca-10cc-4c33-a557-1401c5721e3b.jpg?1562934258"
    }
}
