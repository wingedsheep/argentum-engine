package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eternal Isolation — Core Set 2020 #15
 * {1}{W} · Sorcery · Uncommon
 *
 * Put target creature with power 4 or greater on the bottom of its owner's library.
 *
 * A forced tuck (no top/bottom choice). The power restriction reads projected power, so it is
 * checked on cast and again on resolution — a creature shrunk below 4 in response is no longer a
 * legal target and the spell fizzles.
 */
val EternalIsolation = card("Eternal Isolation") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put target creature with power 4 or greater on the bottom of its owner's library."

    spell {
        val t = target(TargetFilter.Creature.powerAtLeast(4))
        effect = Effects.PutOnBottomOfLibrary(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Johan Grenier"
        flavorText = "\"Well, at least it's a lovely view.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/021963bf-87fc-432f-b9a1-4f627885a7a9.jpg?1783933031"
    }
}
