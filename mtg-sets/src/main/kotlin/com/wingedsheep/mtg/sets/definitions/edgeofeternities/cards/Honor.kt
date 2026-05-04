package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets

/**
 * Honor
 * {W}
 * Sorcery
 * Put a +1/+1 counter on target creature.
 * Draw a card.
 */
val Honor = card("Honor") {
    manaCost = "{W}"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature.\nDraw a card."

    // Spell effect: put +1/+1 counter on target creature and draw a card
    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.AddCounters(com.wingedsheep.sdk.core.Counters.PLUS_ONE_PLUS_ONE, 1, creature)
            .then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Eli Minaya"
        flavorText = "\"With this mantle, you carry the duty to spread light, liberty, and the grace of Taman IV to the stars.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0b4e925-1d59-41a1-bc06-9982695d778f.jpg?1752946636"
    }
}
