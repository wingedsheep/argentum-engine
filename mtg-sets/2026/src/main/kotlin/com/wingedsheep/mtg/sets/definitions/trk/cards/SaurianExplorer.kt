package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Saurian Explorer
 * {3}{G}
 * Creature — Dinosaur Scout
 * 4/3
 *
 * Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn.
 * {6}: Create a tapped Planet land token named New Planet with "{T}: Add one mana of any color."
 * Activate only as a sorcery.
 */
val SaurianExplorer = card("Saurian Explorer") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Dinosaur Scout"
    power = 4
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn.\n{6}: Create a tapped Planet land token named New Planet with \"{T}: Add one mana of any color.\" Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{6}")
        effect = Effects.CreateNewPlanet()
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "213"
        artist = "Bartek Fedyczak"
        flavorText = "The Voth are one of the Galaxy's oldest spacefaring species."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/197abe5e-cde2-4b29-a10f-d6b789776bfa.jpg?1785981598"
    }
}
