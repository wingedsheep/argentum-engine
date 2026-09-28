package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Harmony of Nature
 * {2}{G}
 * Sorcery
 * Tap any number of untapped creatures you control. You gain 4 life for each creature tapped
 * this way.
 *
 * Gather your untapped creatures, choose any number (zero allowed), tap them, then gain
 * 4 x the selection count.
 */
val HarmonyOfNature = card("Harmony of Nature") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Tap any number of untapped creatures you control. You gain 4 life for each " +
        "creature tapped this way."

    spell {
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.youControl().untapped()
                )
            )
            val tapped = chooseAnyNumber(
                from = candidates,
                prompt = "Tap any number of untapped creatures you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(tapped, tap = true))
            run(Effects.GainLife(tapped.count * 4))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3fa08d9-d41b-4696-b81a-42c8eebdeb49.jpg?1783946457"
    }
}
