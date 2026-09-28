package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rain of Daggers
 * {4}{B}{B}
 * Sorcery
 * Destroy all creatures target opponent controls. You lose 2 life for each creature destroyed this way.
 */
val RainOfDaggers = card("Rain of Daggers") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures target opponent controls. You lose 2 life for each creature destroyed this way."

    spell {
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val destroyed = runStoringCollection {
                Effects.DestroyAll(GameObjectFilter.Creature.targetOpponentControls(), storeDestroyedAs = it)
            }
            run(Effects.LoseLife(destroyed.count * 2, EffectTarget.Controller))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Melissa A. Benson"
        flavorText = "Knives in the sky, cries in the air, and blood on the ground."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb09a5bb-9730-43cd-8dea-3842634c9983.jpg?1783946470"
    }
}
