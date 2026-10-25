package com.wingedsheep.mtg.sets.definitions.lcc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ripples of Potential
 * {1}{U}
 * Instant
 * Proliferate, then choose any number of permanents you control that had a counter put on them this
 * way. Those permanents phase out.
 *
 * - The proliferate publishes the permanents it actually gave a counter (`storeAs`); the follow-up
 *   narrows them to the ones you control and lets you pick any number (zero included) on the
 *   battlefield, then each chosen permanent phases out. An opponent's permanent you proliferated is
 *   never offered.
 */
val RipplesOfPotential = card("Ripples of Potential") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Proliferate, then choose any number of permanents you control that had a counter put on " +
        "them this way. Those permanents phase out. (To proliferate, choose any number of permanents and/or " +
        "players, then give each another counter of each kind already there. Treat phased-out permanents and " +
        "anything attached to them as though they don't exist until their controller's next turn.)"

    spell {
        effect = Effects.Pipeline {
            val countered = runStoringCollection { Effects.Proliferate(storeAs = it) }
            val chosen = chooseAnyNumber(
                from = countered,
                filter = GameObjectFilter.Permanent.youControl(),
                prompt = "Choose any number of permanents you control that got a counter to phase out",
                selectedLabel = "Phase out",
                remainderLabel = "Stay",
                useTargetingUI = true,
            )
            run(Effects.ForEachInCollection(chosen, Effects.PhaseOut(EffectTarget.IterationEntity)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Sam Hogg"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38a1b5dd-73e7-4f9f-995f-a23eabafe134.jpg?1783913922"
        ruling("2023-11-10", "You don't have to choose every permanent or player that has a counter, only the ones you want to add another counter to. Since \"any number\" includes zero, you don't have to choose any permanents at all, and you don't have to choose any players at all.")
        ruling("2023-11-10", "As a creature is phased out, Auras and Equipment attached to it also phase out at the same time. Those Auras and Equipment will phase in at the same time that creature does, and they'll phase in still attached to that creature.")
        ruling("2023-11-10", "If a permanent had counters on it when it phased out, it will have those counters when it phases back in.")
        ruling("2023-11-10", "An attacking or blocking creature that phases out is removed from combat.")
    }
}
