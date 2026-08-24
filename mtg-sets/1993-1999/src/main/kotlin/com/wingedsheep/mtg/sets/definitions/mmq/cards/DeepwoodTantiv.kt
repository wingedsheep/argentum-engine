package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deepwood Tantiv
 * {4}{G}
 * Creature — Beast
 * 2 / 4
 *
 * Whenever this creature becomes blocked, you gain 2 life.
 */
val DeepwoodTantiv = card("Deepwood Tantiv") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    oracleText = "Whenever this creature becomes blocked, you gain 2 life."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Joel Biske"
        flavorText = "A single tantiv is just as dangerous as a herd."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfa2028e-4e73-4ff2-a9e2-9ac347d67893.jpg"
    }
}
