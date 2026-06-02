package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Horses of the Bruinen
 * {3}{U}{U}
 * Sorcery
 *
 * Return up to two target creatures to their owners' hands. Scry 1. The Ring tempts you.
 */
val HorsesOfTheBruinen = card("Horses of the Bruinen") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creatures to their owners' hands. Scry 1. The Ring tempts you."

    spell {
        target("up to two target creatures", Targets.UpToCreatures(2))
        effect = ForEachTargetEffect(
            listOf(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
        )
            .then(LibraryPatterns.scry(1))
            .then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Kasia 'Kafis' Zielińska"
        flavorText = "The black horses were filled with madness, and leaping forward in terror they bore their riders into the rushing flood."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7725dc2-2654-4ffb-b6b3-510ae64ec6af.jpg?1686968142"
    }
}
