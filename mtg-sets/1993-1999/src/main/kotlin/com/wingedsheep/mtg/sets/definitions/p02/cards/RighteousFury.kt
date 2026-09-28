package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Righteous Fury
 * {4}{W}{W}
 * Sorcery
 * Destroy all tapped creatures. You gain 2 life for each creature destroyed this way.
 *
 * Life gain counts only creatures actually destroyed (regeneration survivors don't count),
 * via [Effects.DestroyAll]'s storeDestroyedAs (mirrors Fumigate).
 */
val RighteousFury = card("Righteous Fury") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy all tapped creatures. You gain 2 life for each creature destroyed this way."

    spell {
        effect = Effects.Pipeline {
            val destroyed = runStoringCollection {
                Effects.DestroyAll(GameObjectFilter.Creature.tapped(), storeDestroyedAs = it)
            }
            run(Effects.GainLife(destroyed.count * 2))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c408f43e-9092-440d-a15f-bef4ad58bcc6.jpg?1783946490"
    }
}
