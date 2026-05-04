package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Icecave Crasher
 * {3}{G}
 * Creature — Beast
 * Trample
 * Landfall — Whenever a land you control enters, this creature gets +1/+0 until end of turn.
 */
val IcecaveCrasher = card("Icecave Crasher") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Trample\nLandfall — Whenever a land you control enters, this creature gets +1/+0 until end of turn."

    // Trample keyword
    keywords(Keyword.TRAMPLE)

    // Landfall triggered ability: +1/+0 until end of turn when land enters
    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "191"
        artist = "Julia Metzger"
        flavorText = "As warmth crept across Evendo, many long-dormant species grew bolder."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6c1ed0c-0c0d-47a7-8ebc-67854cb226e0.jpg?1752947334"
    }
}
