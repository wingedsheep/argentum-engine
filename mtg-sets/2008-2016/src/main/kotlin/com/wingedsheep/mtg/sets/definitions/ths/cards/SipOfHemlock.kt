package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sip of Hemlock
 * {4}{B}{B}
 * Sorcery
 * Destroy target creature. Its controller loses 2 life.
 */
val SipOfHemlock = card("Sip of Hemlock") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. Its controller loses 2 life."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Move(creature, Zone.GRAVEYARD, byDestruction = true) then
            Effects.LoseLife(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Nils Hamm"
        flavorText = "Conspirators poisoned the oracle not because her visions were wrong, but because they were right."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22051427-9b2a-4571-8c9f-ee84d8d0e4d1.jpg?1783939771"
    }
}
