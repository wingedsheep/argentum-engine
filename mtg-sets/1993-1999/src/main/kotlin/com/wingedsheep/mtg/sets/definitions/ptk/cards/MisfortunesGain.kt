package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.OwnerGainsLifeEffect

/**
 * Misfortune's Gain
 * {3}{W}
 * Sorcery
 * Destroy target creature. Its owner gains 4 life.
 */
val MisfortunesGain = card("Misfortune's Gain") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. Its owner gains 4 life."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.Composite(
            Effects.Destroy(t),
            OwnerGainsLifeEffect(4)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Jiaming"
        flavorText = "The mourning families of commanders and generals were often given land, valuables, or money to compensate for their losses."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80abd7c1-8f7a-4279-b76f-251a02624345.jpg"
    }
}
