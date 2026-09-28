package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Overwhelming Forces
 * {6}{B}{B}
 * Sorcery
 * Destroy all creatures target opponent controls. Draw a card for each creature destroyed this way.
 */
val OverwhelmingForces = card("Overwhelming Forces") {
    manaCost = "{6}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures target opponent controls. Draw a card for each creature destroyed this way."

    spell {
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val destroyed = runStoringCollection {
                Effects.DestroyAll(GameObjectFilter.Creature.targetOpponentControls(), storeDestroyedAs = it)
            }
            run(Effects.DrawCards(destroyed.count, EffectTarget.Controller))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Gao Yan"
        flavorText = "By the year 208, Cao Cao commanded more than 1,000 experienced generals and a million infantry, cavalry, and naval troops."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c56c7fb4-8b7b-40fc-879c-76cfb5d417b8.jpg?1783946115"
    }
}
