package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cartographer's Companion — {3}
 * Artifact Creature — Gnome
 * 2/1
 * When this creature enters, create a Map token.
 */
val CartographersCompanion = card("Cartographer's Companion") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Gnome"
    oracleText = "When this creature enters, create a Map token. (It's an artifact with \"{1}, {T}, Sacrifice this token: Target creature you control explores. Activate only as a sorcery.\")"
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateMapToken()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "248"
        artist = "Chuck Lukacs"
        flavorText = "Ever since the Fomori's attempt long ago to extinguish Chimil's light, the Oltec have taken extensive measures to never get lost in the Core."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbd8115e-4cb3-49b6-b74f-8fcfa28c6404.jpg?1782694413"
    }
}
