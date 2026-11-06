package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ravenous Intruder
 * {1}{R}
 * Creature — Gremlin
 * 1/2
 * Sacrifice an artifact: This creature gets +2/+2 until end of turn.
 */
val RavenousIntruder = card("Ravenous Intruder") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Gremlin"
    power = 1
    toughness = 2
    oracleText = "Sacrifice an artifact: This creature gets +2/+2 until end of turn."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Artifact)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Mathias Kollros"
        flavorText = "\"I've made some fine acquisitions of late, so I'm planning to make commensurate improvements to my security systems. I have a consultant coming tomorrow.\"\n—Kurna Majan, Luminary League"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd793778-f099-4b8e-af19-fc0ec4292824.jpg"
    }
}
