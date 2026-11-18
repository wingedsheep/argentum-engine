package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thoughtrender Lamia
 * {4}{B}{B}
 * Enchantment Creature — Lamia
 * 5/3
 *
 * Constellation — Whenever this creature or another enchantment you control enters, each opponent discards a card.
 */
val ThoughtrenderLamia = card("Thoughtrender Lamia") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Lamia"
    power = 5
    toughness = 3
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, each opponent discards a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Patterns.Hand.eachOpponentDiscards(1)
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "each opponent discards a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Magali Villeneuve"
        flavorText = "Some predators can sense fear in their prey, but the lamia is drawn to madness."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69799971-6e24-4b90-ba72-44c6393fc3c4.jpg?1783939429"
    }
}
