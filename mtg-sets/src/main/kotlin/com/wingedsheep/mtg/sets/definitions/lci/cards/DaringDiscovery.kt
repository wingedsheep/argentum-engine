package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Daring Discovery
 * {4}{R}
 * Sorcery
 * Up to three target creatures can't block this turn.
 * Discover 4.
 */
val DaringDiscovery = card("Daring Discovery") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Up to three target creatures can't block this turn.\nDiscover 4."
    spell {
        target("up to three target creatures", TargetCreature(count = 3, optional = true))
        effect = Effects.Composite(
            ForEachTargetEffect(listOf(Effects.CantBlock(EffectTarget.ContextTarget(0)))),
            Effects.Discover(4)
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Michele Giorgi"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d95018a4-be10-4c46-b14d-4b1eba838bc0.jpg?1782694496"
    }
}
