package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skyclave Geopede — Zendikar Rising #163
 * {2}{R} · Creature — Insect · 3/1
 *
 * Trample
 * Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn.
 */
val SkyclaveGeopede = card("Skyclave Geopede") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Insect"
    oracleText = "Trample\n" +
        "Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn."
    power = 3
    toughness = 1

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Filip Burburan"
        flavorText = "Wrenched from the ground like a dirt-caked rock, the ancient Skyclave wriggled with life."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db9103c9-084f-4ad2-8b7b-ca52be97619d.jpg"
    }
}
