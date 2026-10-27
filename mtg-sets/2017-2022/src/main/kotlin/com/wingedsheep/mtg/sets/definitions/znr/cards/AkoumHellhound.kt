package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Akoum Hellhound — Zendikar Rising #133
 * {R} · Creature — Elemental Dog · 0/1
 *
 * Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn.
 */
val AkoumHellhound = card("Akoum Hellhound") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Dog"
    oracleText = "Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn."
    power = 0
    toughness = 1

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Jason Kang"
        flavorText = "Hellhound packs roam the Spikefields, basking in the harsh sunlight and lighting the night with their flames."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a5ad1c8-3d70-43ed-9539-2aa6289c5c4a.jpg"
    }
}
